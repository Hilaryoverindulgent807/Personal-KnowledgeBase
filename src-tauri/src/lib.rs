pub(crate) mod sidecar;
mod tray;

use tauri::{Emitter, Manager};
use std::sync::Mutex;
use tauri_plugin_global_shortcut::ShortcutState;

pub(crate) struct BackendState(pub(crate) Mutex<Option<tokio::process::Child>>);

impl Drop for BackendState {
    fn drop(&mut self) {
        let mut guard = self.0.lock().unwrap();
        if let Some(mut child) = guard.take() {
            println!("[tauri] Dropping BackendState, killing backend process...");
            let _ = child.start_kill();
        }
    }
}

#[tauri::command]
fn get_desktop_config(app: tauri::AppHandle) -> Result<sidecar::DesktopConfig, String> {
    Ok(sidecar::load_config(&app))
}

#[tauri::command]
fn save_desktop_config(app: tauri::AppHandle, config: sidecar::DesktopConfig) -> Result<(), String> {
    sidecar::save_config(&app, &config)
}

#[tauri::command]
fn backend_status(state: tauri::State<'_, BackendState>) -> String {
    let guard = state.0.lock().unwrap();
    if guard.is_some() {
        "running".to_string()
    } else {
        "stopped".to_string()
    }
}

#[tauri::command]
async fn start_backend(
    app: tauri::AppHandle,
    state: tauri::State<'_, BackendState>,
) -> Result<String, String> {
    let mut guard = state.0.lock().unwrap();
    if guard.is_some() {
        return Ok("already running".to_string());
    }

    let child = sidecar::start_spring_boot(&app).map_err(|e| e.to_string())?;
    *guard = Some(child);
    tray::update_tray_icon(&app, true);
    Ok("started".to_string())
}

#[tauri::command]
async fn stop_backend(
    app: tauri::AppHandle,
    state: tauri::State<'_, BackendState>,
) -> Result<String, String> {
    let child = {
        let mut guard = state.0.lock().unwrap();
        guard.take()
    }; // guard dropped before any .await

    if let Some(mut child) = child {
        sidecar::stop_spring_boot(&mut child).await.map_err(|e| e.to_string())?;
        tray::update_tray_icon(&app, false);
        Ok("stopped".to_string())
    } else {
        Ok("not running".to_string())
    }
}

#[tauri::command]
fn open_data_dir(app: tauri::AppHandle) -> Result<(), String> {
    tray::open_data_directory(&app);
    Ok(())
}

/// 检查是否为首次运行（.first-run 文件不存在 = 首次运行）
#[tauri::command]
fn check_first_run(app: tauri::AppHandle) -> Result<bool, String> {
    let app_data = app.path().app_data_dir().map_err(|e| e.to_string())?;
    let first_run_file = app_data.join(".first-run");
    Ok(!first_run_file.exists())
}

/// 标记首次运行已完成（创建 .first-run 文件）
#[tauri::command]
fn mark_first_run_complete(app: tauri::AppHandle) -> Result<(), String> {
    let app_data = app.path().app_data_dir().map_err(|e| e.to_string())?;
    let _ = std::fs::create_dir_all(&app_data);
    let first_run_file = app_data.join(".first-run");
    std::fs::write(&first_run_file, "1").map_err(|e| e.to_string())?;
    println!("[tauri] First run marked complete: {:?}", first_run_file);
    Ok(())
}

/// 重置所有数据（删除数据库、上传文件、向量索引、配置文件、.first-run 标志）
/// 用于"恢复出厂设置"或数据清理场景
#[tauri::command]
async fn reset_all_data(
    app: tauri::AppHandle,
    state: tauri::State<'_, BackendState>,
) -> Result<String, String> {
    // 1. 先停止后端
    let child = {
        let mut guard = state.0.lock().unwrap();
        guard.take()
    };
    if let Some(mut child) = child {
        let _ = crate::sidecar::stop_spring_boot(&mut child).await;
        tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
    }

    // 2. 获取数据目录
    let config = sidecar::load_config(&app);
    let default_app_data = app.path().app_data_dir().map_err(|e| e.to_string())?;
    let app_data_dir = if let Some(custom) = &config.custom_data_dir {
        if !custom.trim().is_empty() {
            std::path::PathBuf::from(custom)
        } else {
            default_app_data
        }
    } else {
        default_app_data
    };

    // 3. 删除所有数据文件
    let items_to_delete = [
        app_data_dir.join("intelligence_platform.db"),
        app_data_dir.join("intelligence_platform.db-shm"),
        app_data_dir.join("intelligence_platform.db-wal"),
        app_data_dir.join("vector-index.json"),
        app_data_dir.join("config.json"),
        app_data_dir.join(".first-run"),
    ];

    for item in &items_to_delete {
        if item.exists() {
            if item.is_dir() {
                let _ = std::fs::remove_dir_all(item);
            } else {
                let _ = std::fs::remove_file(item);
            }
            println!("[tauri] Deleted: {:?}", item);
        }
    }

    // 4. 删除 uploads 目录
    let uploads_dir = app_data_dir.join("uploads");
    if uploads_dir.exists() {
        let _ = std::fs::remove_dir_all(&uploads_dir);
        println!("[tauri] Deleted uploads dir: {:?}", uploads_dir);
    }

    // 5. 重新创建 uploads 目录
    let _ = std::fs::create_dir_all(&uploads_dir);

    // 6. 重新启动后端
    let child = sidecar::start_spring_boot(&app).map_err(|e| e.to_string())?;
    let mut guard = state.0.lock().unwrap();
    *guard = Some(child);
    tray::update_tray_icon(&app, true);

    Ok("reset_complete".to_string())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.show();
                let _ = window.set_focus();
            }
        }))
        .plugin(tauri_plugin_updater::Builder::new().build())
        .plugin(tauri_plugin_global_shortcut::Builder::new().build())
        .manage(BackendState(Mutex::new(None)))
        .invoke_handler(tauri::generate_handler![
            backend_status,
            start_backend,
            stop_backend,
            open_data_dir,
            get_desktop_config,
            save_desktop_config,
            check_first_run,
            mark_first_run_complete,
            reset_all_data,
        ])
        .setup(|app| {
            // 初始化系统托盘
            let app_handle = app.handle().clone();
            if let Err(e) = tray::setup_tray(&app_handle) {
                eprintln!("[tauri] Failed to setup tray: {}", e);
            }

            // 注册全局快捷键
            use tauri_plugin_global_shortcut::GlobalShortcutExt;

            // CmdOrCtrl+Q: 退出应用
            if let Err(e) = app_handle.global_shortcut().on_shortcut("CmdOrCtrl+Q", |app, _, event| {
                if event.state == ShortcutState::Pressed {
                    app.exit(0);
                }
            }) {
                eprintln!("[tauri] Failed to register shortcut CmdOrCtrl+Q: {}", e);
            }

            // CmdOrCtrl+W: 隐藏当前窗口到托盘
            if let Err(e) = app_handle.global_shortcut().on_shortcut("CmdOrCtrl+W", |app, _, event| {
                if event.state == ShortcutState::Pressed {
                    if let Some(window) = app.get_webview_window("main") {
                        let _ = window.hide();
                    }
                }
            }) {
                eprintln!("[tauri] Failed to register shortcut CmdOrCtrl+W: {}", e);
            }

            // CmdOrCtrl+,: 打开设置页面
            if let Err(e) = app_handle.global_shortcut().on_shortcut("CmdOrCtrl+,", |app, _, event| {
                if event.state == ShortcutState::Pressed {
                    let _ = app.emit("open-settings", ());
                }
            }) {
                eprintln!("[tauri] Failed to register shortcut CmdOrCtrl+,: {}", e);
            }

            // 应用启动时自动启动 Spring Boot 后端
            tauri::async_runtime::spawn(async move {
                tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
                match sidecar::start_spring_boot(&app_handle) {
                    Ok(child) => {
                        let state: tauri::State<'_, BackendState> = app_handle.state();
                        let mut guard = state.0.lock().unwrap();
                        *guard = Some(child);
                        println!("[tauri] Spring Boot backend started");
                        tray::update_tray_icon(&app_handle, true);
                    }
                    Err(e) => {
                        eprintln!("[tauri] Failed to start backend: {}", e);
                        tray::update_tray_icon(&app_handle, false);
                    }
                }
            });
            Ok(())
        })
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                let app = window.app_handle();
                let config = sidecar::load_config(app);
                let close_action = config.close_action.unwrap_or_else(|| "minimize".to_string());
                if close_action == "minimize" {
                    api.prevent_close();
                    let _ = window.hide();
                }
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
