use tauri::{
    image::Image,
    menu::{MenuBuilder, MenuItemBuilder},
    tray::TrayIconBuilder,
    AppHandle, Manager,
};
use tauri_plugin_opener::OpenerExt;

use crate::BackendState;

const TRAY_ID: &str = "main_tray";

/// 初始化系统托盘图标和菜单
pub fn setup_tray(app: &AppHandle) -> Result<(), Box<dyn std::error::Error>> {
    let icon = app
        .default_window_icon()
        .ok_or("No default window icon available")?
        .clone();

    let show_hide = MenuItemBuilder::new("显示/隐藏主窗口")
        .id("show_hide")
        .build(app)?;
    let restart = MenuItemBuilder::new("重启后端")
        .id("restart_backend")
        .build(app)?;
    let open_dir = MenuItemBuilder::new("打开数据目录")
        .id("open_data_dir")
        .build(app)?;
    let quit = MenuItemBuilder::new("退出应用")
        .id("quit")
        .build(app)?;

    let menu = MenuBuilder::new(app)
        .item(&show_hide)
        .separator()
        .item(&restart)
        .item(&open_dir)
        .separator()
        .item(&quit)
        .build()?;

    TrayIconBuilder::with_id(TRAY_ID)
        .icon(icon)
        .menu(&menu)
        .tooltip("智能情报分析平台")
        .on_menu_event(|app, event| match event.id().as_ref() {
            "show_hide" => toggle_window_visibility(&app),
            "restart_backend" => restart_backend(&app),
            "open_data_dir" => open_data_directory(&app),
            "quit" => app.exit(0),
            _ => {}
        })
        .build(app)?;

    Ok(())
}

/// 更新托盘图标以反映后端状态
/// - `running = true`：显示正常图标
/// - `running = false`：显示警告图标（若可用），否则保持默认图标
pub fn update_tray_icon(app: &AppHandle, running: bool) {
    let Some(tray) = app.tray_by_id(TRAY_ID) else {
        return;
    };

    if running {
        if let Some(icon) = app.default_window_icon() {
            let _ = tray.set_icon(Some(icon.clone()));
        }
    } else {
        let warning_icon = load_warning_icon(app);
        let _ = tray.set_icon(Some(warning_icon));
    }
}

// ── private helpers ──────────────────────────────────────────────

/// 切换主窗口的显示/隐藏
fn toggle_window_visibility(app: &AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        if window.is_visible().unwrap_or(false) {
            let _ = window.hide();
        } else {
            let _ = window.show();
            let _ = window.set_focus();
        }
    }
}

/// 重启后端：先优雅停止再启动
fn restart_backend(app: &AppHandle) {
    let app_handle = app.clone();
    tauri::async_runtime::spawn(async move {
        // 先优雅停止后端
        let child = {
            let state: tauri::State<'_, BackendState> = app_handle.state();
            let mut guard = state.0.lock().unwrap();
            guard.take()
        }; // guard dropped before any .await

        if let Some(mut child) = child {
            let _ = crate::sidecar::stop_spring_boot(&mut child).await;
            println!("[tray] Backend stopped for restart");
        }

        // 短暂等待后再启动
        tokio::time::sleep(tokio::time::Duration::from_secs(1)).await;

        // 重新启动后端
        match crate::sidecar::start_spring_boot(&app_handle) {
            Ok(child) => {
                let state: tauri::State<'_, BackendState> = app_handle.state();
                let mut guard = state.0.lock().unwrap();
                *guard = Some(child);
                println!("[tray] Backend restarted successfully");
                update_tray_icon(&app_handle, true);
            }
            Err(e) => {
                eprintln!("[tray] Failed to restart backend: {}", e);
                update_tray_icon(&app_handle, false);
            }
        }
    });
}

/// 使用系统文件管理器打开数据目录
pub fn open_data_directory(app: &AppHandle) {
    let config = crate::sidecar::load_config(app);
    let data_dir = if let Some(custom) = &config.custom_data_dir {
        if !custom.trim().is_empty() {
            std::path::PathBuf::from(custom)
        } else {
            app.path().app_data_dir().unwrap_or_default()
        }
    } else {
        app.path().app_data_dir().unwrap_or_default()
    };

    if data_dir.as_os_str().is_empty() {
        eprintln!("[tray] Data directory path is empty");
        return;
    }

    let _ = std::fs::create_dir_all(&data_dir);
    if let Err(e) = app
        .opener()
        .open_path(data_dir.to_string_lossy().to_string(), None::<&str>)
    {
        eprintln!("[tray] Failed to open data directory: {}", e);
    }
}

/// 加载警告状态图标，失败时回退到默认图标
fn load_warning_icon(app: &AppHandle) -> Image<'_> {
    // 使用默认图标
    app.default_window_icon()
        .cloned()
        .expect("default window icon should be configured in tauri.conf.json")
}
