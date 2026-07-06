<template>
  <div class="qa-page">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header><span>智能问答</span></template>
          <div class="chat-area">
            <div v-for="(msg, i) in messages" :key="i" :class="['chat-bubble', msg.role]">
              <div class="bubble-content">{{ msg.content }}</div>
              <div v-if="msg.confidence" class="confidence">置信度: {{ (msg.confidence * 100).toFixed(0) }}%</div>
            </div>
            <el-empty v-if="messages.length === 0" description="输入问题开始对话" />
          </div>
          <div class="input-area">
            <el-input v-model="question" placeholder="输入您的问题" @keyup.enter="askQuestion" />
            <el-button type="primary" @click="askQuestion" :loading="asking" style="margin-left: 10px;">提问</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>历史会话</span></template>
          <div v-for="s in sessions" :key="s.session_id" class="session-item" @click="loadSession(s.session_id)">
            <span>{{ s.title }}</span>
            <el-tag size="small">{{ s.msg_count }}条</el-tag>
          </div>
          <el-empty v-if="sessions.length === 0" description="暂无历史会话" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createQA, getQASessions, getQASession } from '../../api'

interface Message { role: 'user' | 'assistant'; content: string; confidence?: number }

const messages = ref<Message[]>([])
const question = ref('')
const asking = ref(false)
const sessions = ref<any[]>([])
const sessionId = ref('session_' + Date.now())

async function askQuestion() {
  if (!question.value.trim()) return
  asking.value = true
  const q = question.value
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  try {
    const res = await createQA({
      question: q,
      answer: '基于当前知识库的分析结果：该问题需要进一步的数据支持。系统已记录您的问题，将在后续更新中提供详细分析。',
      confidence: 0.75,
      sources: '[]',
      user_name: '用户',
      session_id: sessionId.value,
      category: '',
    })
    messages.value.push({ role: 'assistant', content: res.data.message || '已记录您的问题。', confidence: 0.75 })
    loadSessions()
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '抱歉，系统暂时无法处理您的请求。' })
  }
  asking.value = false
}

async function loadSessions() {
  try {
    const res = await getQASessions()
    sessions.value = res.data
  } catch (e) { console.error(e) }
}

async function loadSession(sid: string) {
  try {
    const res = await getQASession(sid)
    messages.value = []
    sessionId.value = sid
    for (const item of res.data) {
      messages.value.push({ role: 'user', content: item.question })
      messages.value.push({ role: 'assistant', content: item.answer, confidence: item.confidence })
    }
  } catch (e) { console.error(e) }
}

onMounted(loadSessions)
</script>

<style scoped>
.qa-page { padding: 20px; }
.chat-area { max-height: 500px; overflow-y: auto; margin-bottom: 16px; }
.chat-bubble { margin-bottom: 12px; display: flex; }
.chat-bubble.user { justify-content: flex-end; }
.bubble-content { max-width: 70%; padding: 10px 16px; border-radius: 12px; }
.user .bubble-content { background: #409eff; color: white; }
.assistant .bubble-content { background: #f4f4f5; }
.confidence { font-size: 12px; color: #909399; margin-top: 4px; }
.input-area { display: flex; }
.session-item { display: flex; justify-content: space-between; padding: 8px; cursor: pointer; border-bottom: 1px solid #ebeef5; }
.session-item:hover { background: #f5f7fa; }
</style>
