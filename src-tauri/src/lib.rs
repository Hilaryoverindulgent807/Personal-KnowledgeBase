mod sidecar;

use std::sync::Mutex;
use tauri::Manager;

struct BackendState(Mutex<Option<tokio::process::Child>>);

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
    Ok("started".to_string())
}

#[tauri::command]
async fn stop_backend(state: tauri::State<'_, BackendState>) -> Result<String, String> {
    let mut guard = state.0.lock().unwrap();
    if let Some(mut child) = guard.take() {
        child.kill().await.map_err(|e| e.to_string())?;
        Ok("stopped".to_string())
    } else {
        Ok("not running".to_string())
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_opener::init())
        .plugin(tauri_plugin_shell::init())
        .manage(BackendState(Mutex::new(None)))
        .invoke_handler(tauri::generate_handler![
            backend_status,
            start_backend,
            stop_backend,
        ])
        .setup(|app| {
            // 应用启动时自动启动 Spring Boot 后端
            let app_handle = app.handle().clone();
            tauri::async_runtime::spawn(async move {
                tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
                match sidecar::start_spring_boot(&app_handle) {
                    Ok(child) => {
                        let state: tauri::State<'_, BackendState> = app_handle.state();
                        let mut guard = state.0.lock().unwrap();
                        *guard = Some(child);
                        println!("[tauri] Spring Boot backend started");
                    }
                    Err(e) => {
                        eprintln!("[tauri] Failed to start backend: {}", e);
                    }
                }
            });
            Ok(())
        })
        .on_window_event(|window, event| {
            // 主窗口关闭时停止后端
            if let tauri::WindowEvent::CloseRequested { .. } = event {
                let state: tauri::State<'_, BackendState> = window.state();
                let mut guard = state.0.lock().unwrap();
                if let Some(mut child) = guard.take() {
                    let _ = child.kill();
                    println!("[tauri] Spring Boot backend stopped");
                }
            }
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
