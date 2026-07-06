<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { isTauri } from './composables/useTauri'
import SplashScreen from './components/SplashScreen.vue'
import UpdateDialog from './components/UpdateDialog.vue'
import FirstRunWizard from './components/FirstRunWizard.vue'
import ErrorBoundary from './components/ErrorBoundary.vue'

const isReady = ref(false)
const router = useRouter()

let unlisten: (() => void) | null = null

onMounted(async () => {
  if (isTauri()) {
    try {
      const { listen } = await import('@tauri-apps/api/event')
      unlisten = await listen('open-settings', () => {
        router.push('/admin/settings')
      })
    } catch (e) {
      console.error('Failed to register open-settings listener:', e)
    }
  }
})

onUnmounted(() => {
  unlisten?.()
})
</script>

<template>
  <!-- 启动加载界面：backendStatus === 'running' 时隐藏 -->
  <SplashScreen @ready="isReady = true" />
  <FirstRunWizard />
  <UpdateDialog />
  <!-- 主界面：SplashScreen 消失后 fade-in 出现 -->
  <ErrorBoundary>
    <Transition name="fade-in" appear>
      <router-view v-show="isReady" />
    </Transition>
  </ErrorBoundary>
</template>

<style>
/* Global resets or styling can go here */
body {
  margin: 0;
  padding: 0;
  overflow: hidden;
  background-color: #0d0f15;
}

/* 主界面 fade-in 过渡动画 */
.fade-in-enter-active {
  transition: opacity 0.6s ease-out;
}
.fade-in-enter-from {
  opacity: 0;
}
.fade-in-enter-to {
  opacity: 1;
}
</style>
