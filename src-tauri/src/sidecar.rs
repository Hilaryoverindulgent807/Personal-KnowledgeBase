use std::process::Stdio;
use tauri::AppHandle;
use tauri::Manager;
use tokio::process::{Child, Command};

#[derive(serde::Serialize, serde::Deserialize, Default, Clone, Debug)]
pub struct DesktopConfig {
    pub custom_data_dir: Option<String>,
    pub close_action: Option<String>, // "minimize" or "quit"
}

pub fn get_config_path(app: &AppHandle) -> Result<std::path::PathBuf, String> {
    let app_data = app.path().app_data_dir().map_err(|e| e.to_string())?;
    Ok(app_data.join("config.json"))
}

pub fn load_config(app: &AppHandle) -> DesktopConfig {
    if let Ok(path) = get_config_path(app) {
        if path.exists() {
            if let Ok(content) = std::fs::read_to_string(path) {
                if let Ok(config) = serde_json::from_str::<DesktopConfig>(&content) {
                    return config;
                }
            }
        }
    }
    DesktopConfig::default()
}

pub fn save_config(app: &AppHandle, config: &DesktopConfig) -> Result<(), String> {
    let path = get_config_path(app)?;
    if let Some(parent) = path.parent() {
        let _ = std::fs::create_dir_all(parent);
    }
    let content = serde_json::to_string_pretty(config).map_err(|e| e.to_string())?;
    std::fs::write(path, content).map_err(|e| e.to_string())?;
    Ok(())
}

/// Spring Boot JAR 的 sidecar 启动器
/// 支持两种模式：
/// 1. Sidecar 模式：从 Tauri bundle 中加载预打包的 JAR
/// 2. 开发模式：直接通过 mvnw 启动
pub fn start_spring_boot(app: &AppHandle) -> Result<Child, Box<dyn std::error::Error>> {
    let resource_dir = app.path().resource_dir()?;
    let default_app_data_dir = app.path().app_data_dir()?;

    // 加载自定义数据目录配置
    let config = load_config(app);
    let app_data_dir = if let Some(custom) = &config.custom_data_dir {
        if !custom.trim().is_empty() {
            let path = std::path::PathBuf::from(custom);
            let _ = std::fs::create_dir_all(&path);
            path
        } else {
            default_app_data_dir
        }
    } else {
        default_app_data_dir
    };

    // 确保数据目录存在
    std::fs::create_dir_all(&app_data_dir)?;

    // 查找 Java 可执行文件
    let java_cmd = find_java()?;

    // 查找 JAR 文件
    let jar_path = find_jar(&resource_dir)?;

    println!("[sidecar] Starting Spring Boot: {} {}", java_cmd, jar_path);

    let mut cmd = Command::new(&java_cmd);
    cmd.arg("-jar")
        .arg(&jar_path)
        .arg(format!("-Dserver.port={}", get_backend_port()))
        .arg(format!("-Dupload.dir={}", app_data_dir.join("uploads").display()))
        .arg(format!("-Dspring.datasource.url=jdbc:sqlite:{}", app_data_dir.join("intelligence_platform.db").display()))
        .current_dir(&app_data_dir)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());

    let child = cmd.spawn()?;
    println!("[sidecar] Spring Boot process started (PID: {:?})", child.id());
    Ok(child)
}

/// 查找 Java 运行时
fn find_java() -> Result<String, Box<dyn std::error::Error>> {
    // 优先检查 JAVA_HOME
    if let Ok(java_home) = std::env::var("JAVA_HOME") {
        let java_bin = if cfg!(windows) {
            format!("{}/bin/java.exe", java_home)
        } else {
            format!("{}/bin/java", java_home)
        };
        if std::path::Path::new(&java_bin).exists() {
            return Ok(java_bin);
        }
    }

    // 检查 PATH 中的 java
    if std::process::Command::new("java")
        .arg("-version")
        .output()
        .is_ok()
    {
        return Ok("java".to_string());
    }

    Err("Java runtime not found. Please install Java 17+ or set JAVA_HOME.".into())
}

/// 查找 Spring Boot JAR 文件
fn find_jar(resource_dir: &std::path::Path) -> Result<String, Box<dyn std::error::Error>> {
    // 1. 检查候选的直接或子目录路径
    let jar_candidates = [
        "binaries/backend.jar",
        "binaries/intelligence-platform.jar",
        "backend.jar",
        "intelligence-platform.jar",
    ];
    for candidate in &jar_candidates {
        let jar = resource_dir.join(candidate);
        if jar.exists() {
            return Ok(jar.to_string_lossy().to_string());
        }
    }

    // 2. 递归在 resource_dir 及其所有子目录中搜索第一个 .jar 文件
    fn find_jar_recursive(dir: &std::path::Path) -> Option<std::path::PathBuf> {
        if let Ok(entries) = std::fs::read_dir(dir) {
            let mut subdirs = Vec::new();
            for entry in entries.flatten() {
                let path = entry.path();
                if path.is_file() {
                    if path.extension().map_or(false, |ext| ext == "jar") {
                        return Some(path);
                    }
                } else if path.is_dir() {
                    subdirs.push(path);
                }
            }
            for subdir in subdirs {
                if let Some(jar_path) = find_jar_recursive(&subdir) {
                    return Some(jar_path);
                }
            }
        }
        None
    }

    if let Some(jar_path) = find_jar_recursive(resource_dir) {
        return Ok(jar_path.to_string_lossy().to_string());
    }

    Err(format!(
        "Spring Boot JAR not found in {}. Please build the backend first: cd backend-springboot && ./mvnw package",
        resource_dir.display()
    ).into())
}

/// 优雅关闭 Spring Boot 后端进程
/// 1. 先发送 SIGTERM（Unix）或 taskkill（Windows），触发 Spring Boot 的 graceful shutdown
/// 2. 等待最多 10 秒让进程自行退出
/// 3. 如果超时仍未退出，则强制 kill
pub async fn stop_spring_boot(child: &mut Child) -> Result<(), Box<dyn std::error::Error>> {
    let pid = child.id().ok_or("Process already exited")?;
    println!("[sidecar] Sending SIGTERM to Spring Boot (PID: {})", pid);

    // 发送 SIGTERM 信号（Unix）
    #[cfg(unix)]
    {
        let _ = std::process::Command::new("kill")
            .arg("-TERM")
            .arg(pid.to_string())
            .output();
    }

    // Windows: 使用 taskkill（不带 /F 即为温和终止）
    #[cfg(windows)]
    {
        let _ = std::process::Command::new("taskkill")
            .args(["/PID", &pid.to_string()])
            .output();
    }

    // 等待最多 10 秒让进程优雅退出
    let timeout = tokio::time::Duration::from_secs(10);
    match tokio::time::timeout(timeout, child.wait()).await {
        Ok(Ok(status)) => {
            println!("[sidecar] Spring Boot exited gracefully: {}", status);
        }
        Ok(Err(e)) => {
            eprintln!("[sidecar] Error waiting for process exit: {}", e);
            // 尝试强制终止
            let _ = child.kill().await;
        }
        Err(_) => {
            println!("[sidecar] Graceful shutdown timeout (10s) reached, force killing process");
            let _ = child.kill().await;
        }
    }

    Ok(())
}

/// 获取后端端口
fn get_backend_port() -> u16 {
    std::env::var("BACKEND_PORT")
        .ok()
        .and_then(|p| p.parse().ok())
        .unwrap_or(8080)
}
