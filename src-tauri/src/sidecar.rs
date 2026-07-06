use std::process::Stdio;
use tauri::AppHandle;
use tauri::Manager;
use tokio::process::{Child, Command};

/// Spring Boot JAR 的 sidecar 启动器
/// 支持两种模式：
/// 1. Sidecar 模式：从 Tauri bundle 中加载预打包的 JAR
/// 2. 开发模式：直接通过 mvnw 启动
pub fn start_spring_boot(app: &AppHandle) -> Result<Child, Box<dyn std::error::Error>> {
    let resource_dir = app.path().resource_dir()?;
    let app_data_dir = app.path().app_data_dir()?;

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
        let java_bin = format!("{}/bin/java", java_home);
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
    // 在 resource_dir 中查找 JAR
    let jar_candidates = ["backend.jar", "intelligence-platform.jar"];
    for candidate in &jar_candidates {
        let jar = resource_dir.join(candidate);
        if jar.exists() {
            return Ok(jar.to_string_lossy().to_string());
        }
    }

    // 在 resource_dir 的子目录中搜索
    if let Ok(entries) = std::fs::read_dir(resource_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            if path.extension().map_or(false, |ext| ext == "jar") {
                return Ok(path.to_string_lossy().to_string());
            }
        }
    }

    Err(format!(
        "Spring Boot JAR not found in {}. Please build the backend first: cd backend-springboot && ./mvnw package",
        resource_dir.display()
    ).into())
}

/// 获取后端端口
fn get_backend_port() -> u16 {
    std::env::var("BACKEND_PORT")
        .ok()
        .and_then(|p| p.parse().ok())
        .unwrap_or(8080)
}
