<template>
  <div class="home-page">
    <!-- 顶部欢迎区 (Hero Banner) -->
    <div class="hero-section">
      <h1>智能情报分析平台</h1>
      <p>基于知识图谱的智能情报管理与分析系统</p>
    </div>

    <!-- 词条百科主区域 (Wiki Encyclopedia) -->
    <div class="wiki-entries-section">
      <div class="wiki-section-header">
        <h2>词条百科</h2>
        <span class="wiki-subtitle">点击词条可查看详情 · 共 {{ filteredEntries.length }} 条</span>
      </div>

      <!-- 搜索工具栏 -->
      <div class="filter-toolbar">
        <el-row style="width: 100%; margin: 0; align-items: center;">
          <el-col :span="24">
            <el-input
              v-model="searchQuery"
              placeholder="搜索词条名称、内容或关键词..."
              clearable
              :prefix-icon="Search"
              size="small"
            />
          </el-col>
        </el-row>
      </div>

      <!-- 百科词条紧凑列表（参考 llm_wiki knowledge-tree） -->
      <div class="wiki-list" v-loading="loadingEntries">
        <div
          v-for="(item, index) in paginatedEntries"
          :key="item.id"
          class="wiki-list-item"
          :class="{ 'wiki-list-item-selected': selectedEntry?.id === item.id, 'wiki-list-item-even': index % 2 === 1 }"
          @click="showDetail(item)"
        >
          <!-- 1. 图片类型：显示小图标 + 名称 + 类型标签 -->
          <template v-if="item.entryType === 'image'">
            <el-icon class="wiki-type-icon" :style="{ color: getTypeColor(item.entryType) }"><Picture /></el-icon>
            <span class="wiki-list-title" :title="item.title">{{ item.title }}</span>
            <span class="wiki-type-badge" :style="{ color: getTypeColor(item.entryType), backgroundColor: getTypeColor(item.entryType) + '18' }">图片</span>
          </template>

          <!-- 2. 表格类型：显示小图标 + 名称 + 类型标签 -->
          <template v-else-if="item.entryType === 'table'">
            <el-icon class="wiki-type-icon" :style="{ color: getTypeColor(item.entryType) }"><Grid /></el-icon>
            <span class="wiki-list-title" :title="item.title">{{ item.title }}</span>
            <span class="wiki-type-badge" :style="{ color: getTypeColor(item.entryType), backgroundColor: getTypeColor(item.entryType) + '18' }">表格</span>
          </template>

          <!-- 3. 其他类型：显示类型图标 + 名称 + 类型标签 -->
          <template v-else>
            <el-icon class="wiki-type-icon" :style="{ color: getTypeColor(item.entryType) }">
              <component :is="getTypeIcon(item.entryType)" />
            </el-icon>
            <span class="wiki-list-title" :title="item.title">{{ item.title }}</span>
            <span class="wiki-type-badge" :style="{ color: getTypeColor(item.entryType), backgroundColor: getTypeColor(item.entryType) + '18' }">{{ getTypeBadgeLabel(item.entryType) }}</span>
          </template>
        </div>
      </div>

      <!-- 分页组件 -->
      <div class="wiki-pagination" v-if="filteredEntries.length > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          layout="prev, pager, next, total"
          :total="filteredEntries.length"
          background
          small
        />
      </div>
      
      <el-empty v-if="!loadingEntries && filteredEntries.length === 0" description="没有找到匹配的词条数据" />
    </div>

    <!-- 分割线 -->
    <div class="section-divider"></div>

    <!-- 知识库资料列表区域 -->
    <div class="libraries-section">
      <div class="libraries-header">
        <div class="libraries-title-wrap">
          <h2>知识库资料</h2>
          <span class="libraries-subtitle">上传并管理原文档</span>
        </div>
        <el-button type="primary" size="default" @click="showUpload = true">+ 上传新资料</el-button>
      </div>

      <!-- 4个库的列表展示（横向滑动展示图表库，单行列表展示研究报告等文档库） -->
      <div class="lib-list">
        <div v-for="lib in libraries" :key="lib.key" class="lib-row">
          <div class="lib-row-header">
            <span class="lib-row-name">{{ lib.name }}</span>
          </div>
          <div class="lib-row-body">
            <!-- 1. 对于图表库：展示卡片横向滚动，能展示尽可能多的图表，点击查看原始文件，不显示文件名 -->
            <template v-if="lib.key === 'chart'">
              <div v-if="docList[lib.key] && docList[lib.key].length > 0" class="doc-preview horizontal-scroll">
                <div v-for="doc in (docList[lib.key] || [])" :key="doc.id" class="doc-card chart-card" @click="handleDocClick(doc)">
                  <div class="doc-cover-wrapper chart-cover" style="height: 100%; border-bottom: none;">
                    <img :src="getDocFileUrl(doc.id)" class="doc-cover-img" alt="Chart Preview" />
                  </div>
                </div>
              </div>
              <div v-else class="doc-empty">暂无上传图表</div>
            </template>
            
            <!-- 2. 对于其他文档库（研究报告、动态信息库、译丛译著库）：每一行展示一个文档，显示文件类型，点击可查看原始文件或网页链接 -->
            <template v-else>
              <div v-if="docList[lib.key] && docList[lib.key].length > 0" class="doc-list-view">
                <div v-for="doc in (docList[lib.key] || [])" :key="doc.id" class="doc-list-item" @click="handleDocClick(doc)">
                  <el-icon class="doc-file-icon"><Document /></el-icon>
                  <span class="doc-title-link" :title="doc.title">{{ doc.title }}</span>
                  <span 
                    class="doc-type-tag"
                    :style="getFileTypeStyle(getFileType(doc))"
                  >
                    {{ getFileType(doc) }}
                  </span>
                  <span class="doc-time-label">{{ doc.uploadTime }}</span>
                </div>
              </div>
              <div v-else class="doc-empty">暂无上传文档</div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传新资料对话框 -->
    <el-dialog v-model="showUpload" title="上传新资料" width="600px" :close-on-click-modal="!uploading" append-to-body>
      <el-form :model="uploadForm" label-width="100px" v-if="!uploading">
        <el-form-item label="一级分类">
          <el-select v-model="uploadForm.categoryL1" placeholder="选择分类" clearable style="width: 100%;">
            <el-option label="政治" value="政治" />
            <el-option label="军事" value="军事" />
            <el-option label="经济" value="经济" />
            <el-option label="科技" value="科技" />
            <el-option label="社会" value="社会" />
          </el-select>
        </el-form-item>
        <el-form-item label="资料类型">
          <el-select v-model="uploadForm.docType" style="width: 100%;">
            <el-option label="研究报告" value="report" />
            <el-option label="动态信息" value="dynamic" />
            <el-option label="译丛译著" value="translation" />
            <el-option label="图表" value="chart" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源信息">
          <el-input v-model="uploadForm.sourceOrigin" placeholder="可选：论文名、Blog URL、报告出处" clearable />
        </el-form-item>

        <!-- 图表模式：选择来源文档和页码 -->
        <template v-if="uploadForm.docType === 'chart'">
          <el-form-item label="图表分类">
            <el-radio-group v-model="uploadForm.chartType">
              <el-radio value="image">纯图片</el-radio>
              <el-radio value="table">数据表格 (自动OCR)</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="来源文档">
            <el-select v-model="uploadForm.sourceDocId" placeholder="选择已有文档（可选）" clearable filterable style="width: 100%">
              <el-option-group v-for="lib in libraries" :key="lib.key" :label="lib.name">
                <el-option
                  for="doc in docList[lib.key] || []"
                  v-for="doc in docList[lib.key] || []"
                  :key="doc.id"
                  :label="doc.title"
                  :value="doc.id"
                />
              </el-option-group>
            </el-select>
            <div style="font-size: 11px; color: #94A3B8; margin-top: 2px;">
              如来源文档不在列表中，可先上传该文档再关联
            </div>
          </el-form-item>
          <el-form-item label="页码">
            <el-input-number v-model="uploadForm.sourcePage" :min="1" :max="9999" placeholder="来源页码" controls-position="right" />
            <span style="font-size: 11px; color: #94A3B8; margin-left: 8px;">可选：图表在原文件中的页码</span>
          </el-form-item>
        </template>

        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="5"
            multiple
            drag
          >
            <el-icon style="font-size: 40px; color: #c0c4cc;"><UploadFilled /></el-icon>
            <div>拖拽或点击上传（Word/PDF/TXT/PPT/图片）</div>
          </el-upload>
        </el-form-item>
      </el-form>

      <!-- 处理进度 -->
      <div v-if="uploading" class="upload-progress-area">
        <div class="progress-header">
          <el-icon class="is-loading" style="font-size: 20px; color: #409EFF;"><Loading /></el-icon>
          <span>正在处理文件... ({{ uploadProgress.current }}/{{ uploadProgress.total }})</span>
        </div>
        <el-progress
          :percentage="uploadProgress.percentage"
          :stroke-width="10"
          style="margin: 16px 0"
        />
        <div class="progress-steps">
          <div :class="['step', { active: uploadProgress.step >= 1, done: uploadProgress.step > 1 }]">
            <span class="step-dot" /> 文件上传
          </div>
          <div :class="['step', { active: uploadProgress.step >= 2, done: uploadProgress.step > 2 }]">
            <span class="step-dot" /> 内容解析
          </div>
          <div :class="['step', { active: uploadProgress.step >= 3, done: uploadProgress.step > 3 }]">
            <span class="step-dot" /> LLM 知识抽取
          </div>
          <div :class="['step', { active: uploadProgress.step >= 4 }]">
            <span class="step-dot" /> 完成
          </div>
        </div>
        <div v-if="uploadProgress.currentFile" class="current-file">
          正在处理：{{ uploadProgress.currentFile }}
        </div>
        <div v-if="uploadProgress.log.length > 0" class="progress-log">
          <div v-for="(line, i) in uploadProgress.log" :key="i" class="log-line">{{ line }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showUpload = false" :disabled="uploading">取消</el-button>
        <el-button type="primary" @click="submitUpload" :loading="uploading" :disabled="uploading">
          {{ uploading ? '处理中...' : '上传并抽取' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 词条详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="selectedEntry?.entryType === 'image' || selectedEntry?.entryType === 'table' ? '' : selectedEntry?.title"
      width="750px"
      destroy-on-close
      class="wiki-detail-dialog"
      append-to-body
    >
      <div v-if="selectedEntry" class="entry-detail-container">
        <!-- YAML Frontmatter Panel (参考 llm_wiki FrontmatterPanel) -->
        <div class="wiki-frontmatter-panel">
          <!-- Identity Strip -->
          <div class="fm-identity">
            <div class="fm-icon-box" :style="{ backgroundColor: getTypeStyle(selectedEntry.entryType).bg }">
              <el-icon :size="18" :style="{ color: getTypeStyle(selectedEntry.entryType).color }">
                <component :is="getTypeStyle(selectedEntry.entryType).icon" />
              </el-icon>
            </div>
            <div class="fm-title-area">
              <h3 class="fm-title">{{ selectedEntry.title }}</h3>
              <div class="fm-meta-row">
                <span class="fm-badge-type" :style="{ color: getTypeStyle(selectedEntry.entryType).color, backgroundColor: getTypeStyle(selectedEntry.entryType).bg }">
                  {{ getTypeStyle(selectedEntry.entryType).label }}
                </span>
                <span class="fm-meta-item" v-if="selectedEntry.createdAt">
                  <el-icon style="margin-right: 3px;"><Calendar /></el-icon>
                  {{ selectedEntry.createdAt }}
                </span>
                <template v-if="selectedEntry.keywords">
                  <span v-for="tag in selectedEntry.keywords.split(',').filter(Boolean)" :key="tag" class="fm-meta-tag">
                    <el-icon style="margin-right: 3px;"><PriceTag /></el-icon>
                    {{ tag.trim() }}
                  </span>
                </template>
              </div>
            </div>
          </div>

          <!-- Description -->
          <div class="fm-description" v-if="selectedEntry.description">
            {{ selectedEntry.description }}
          </div>

          <!-- Origin Callout -->
          <div class="fm-origin" v-if="selectedEntry.sourceOrigin">
            <span class="fm-origin-label">Origin:</span> {{ selectedEntry.sourceOrigin }}
          </div>

          <!-- Sources -->
          <div class="fm-section" v-if="selectedEntry.sourceName">
            <div class="fm-section-title">
              <el-icon style="margin-right: 4px;"><Collection /></el-icon>
              Sources
              <span class="fm-section-count">(1)</span>
            </div>
            <div class="fm-sources-list">
              <div class="fm-source-card">
                <span class="fm-source-icon">📄</span>
                <span class="fm-source-name">{{ selectedEntry.sourceName }}</span>
              </div>
            </div>
          </div>

          <!-- Related Links -->
          <div class="fm-section" v-if="selectedEntry.related">
            <div class="fm-section-title">
              <el-icon style="margin-right: 4px;"><Link /></el-icon>
              Related
              <span class="fm-section-count">({{ selectedEntry.related.split(',').filter(Boolean).length }})</span>
            </div>
            <div class="fm-related-chips">
              <span 
                v-for="rel in selectedEntry.related.split(',').filter(Boolean)" 
                :key="rel" 
                class="fm-related-chip"
                @click="navigateToRelated(rel.trim())"
              >
                {{ rel.trim() }}
              </span>
            </div>
          </div>
        </div>

        <el-divider style="margin: 20px 0 16px 0;" />

        <!-- Main Body Content -->
        <div class="wiki-body-content">
          <!-- 1. 图片类型：直接显示原始图片 -->
          <template v-if="selectedEntry.entryType === 'image'">
            <div class="dialog-image-box">
              <img :src="getMediaUrl(selectedEntry.mediaPath || '')" class="detail-image" alt="original-media" />
            </div>
            <div class="image-caption-text" v-if="selectedEntry.content">
              {{ selectedEntry.content }}
            </div>
          </template>

          <!-- 2. 表格类型：直接显示markdown渲染后的结果 -->
          <template v-else-if="selectedEntry.entryType === 'table'">
            <!-- 正常查看状态 -->
            <div v-if="!editMode" class="detail-table-wrapper" style="border: none; padding: 0; background: none;">
              <div class="markdown-table-content markdown-body" v-html="renderMarkdownContent(selectedEntry.tableMarkdown || selectedEntry.content)"></div>
              <div style="margin-top: 20px; text-align: right;">
                <el-button type="primary" size="default" @click="startEditTable">
                  编辑表格数据
                </el-button>
              </div>
            </div>
            <!-- 编辑状态 -->
            <div v-else class="detail-edit-area">
              <el-input
                v-model="editTableMarkdown"
                type="textarea"
                :rows="14"
                placeholder="输入 Markdown 表格内容"
                style="font-family: monospace;"
              />
              <div style="margin-top: 16px;">
                <h4 style="margin-bottom: 8px; color: #475569; font-size: 13px;">实时预览</h4>
                <div class="edit-preview-box markdown-body" v-html="renderMarkdownContent(editTableMarkdown)"></div>
              </div>
              <div style="margin-top: 20px; text-align: right;">
                <el-button size="default" @click="cancelEditTable">取消</el-button>
                <el-button type="primary" size="default" @click="saveTableEdit" :loading="editSaving">
                  保存并同步修改
                </el-button>
              </div>
            </div>
          </template>

          <!-- 3. 其他类型正常显示 Markdown 正文 -->
          <template v-else>
            <div class="entry-content markdown-body" v-html="renderMarkdownContent(selectedEntry.content)"></div>
          </template>
        </div>
      </div>
      <template #footer v-if="!editMode">
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive, watch } from 'vue'
import { Search, UploadFilled, Loading, Picture, Grid } from '@element-plus/icons-vue'
import { getKnowledgeEntries, updateTableMarkdown, getMediaUrl, getDocuments, uploadFile, getDocFileUrl } from '../../api'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
})

interface KnowledgeEntry {
  id: number
  title: string
  entryType: string
  library: string
  documentId?: number
  sourceName?: string
  content: string
  keywords: string
  status: string
  confidence?: number
  createdAt: string
  mediaType?: string
  mediaPath?: string
  sourceOrigin?: string
  tableMarkdown?: string
  projectId?: number
  description?: string
  related?: string
}

// 词条百科数据
const entries = ref<KnowledgeEntry[]>([])
const loadingEntries = ref(false)

// 过滤和搜索条件
const searchQuery = ref('')


// 颜色映射用于分类顶部边框
const typeColors: Record<string, string> = {
  concept: '#c084fc', // purple
  entity: '#60a5fa', // blue
  thesis: '#8b5cf6', // violet
  methodology: '#4ade80', // green
  finding: '#f87171', // red
  comparison: '#ec4899', // pink
  synthesis: '#6366f1', // indigo
  table: '#fbbf24', // amber
  image: '#f43f5e', // rose
}

function getTypeColor(type: string): string {
  return typeColors[type] || '#cbd5e1'
}

function getTypeIcon(type: string): string {
  const iconMap: Record<string, string> = {
    concept: 'Document',
    entity: 'Document',
    thesis: 'Document',
    methodology: 'Document',
    finding: 'Document',
    comparison: 'Document',
    synthesis: 'Document',
  }
  return iconMap[type] || 'Document'
}

function getTypeBadgeLabel(type: string): string {
  const labelMap: Record<string, string> = {
    concept: '概念',
    entity: '实体',
    thesis: '论点',
    methodology: '方法论',
    finding: '发现',
    comparison: '对比',
    synthesis: '综合',
    table: '表格',
    image: '图片',
  }
  return labelMap[type] || '词条'
}

function getFileType(doc: any): string {
  if (!doc) return 'TXT'
  if (doc.url || (doc.title && (doc.title.startsWith('http://') || doc.title.startsWith('https://')))) {
    return 'LINK'
  }
  if (!doc.title) return 'TXT'
  const parts = doc.title.split('.')
  if (parts.length > 1) {
    const ext = parts[parts.length - 1].toUpperCase()
    if (ext === 'HTML' && doc.url) return 'LINK'
    return ext
  }
  if (doc.filePath) {
    const fParts = doc.filePath.split('.')
    return fParts[fParts.length - 1].toUpperCase()
  }
  return 'TXT'
}

function getFileTypeStyle(type: string) {
  const t = (type || '').toUpperCase()
  switch (t) {
    case 'PDF':
      return { color: '#ef4444', backgroundColor: '#fef2f2', border: '1px solid #fee2e2' }
    case 'DOC':
    case 'DOCX':
      return { color: '#3b82f6', backgroundColor: '#eff6ff', border: '1px solid #dbeafe' }
    case 'XLS':
    case 'XLSX':
    case 'CSV':
      return { color: '#10b981', backgroundColor: '#ecfdf5', border: '1px solid #d1fae5' }
    case 'PPT':
    case 'PPTX':
      return { color: '#f59e0b', backgroundColor: '#fffbeb', border: '1px solid #fef3c7' }
    case 'TXT':
    case 'MD':
    case 'MARKDOWN':
      return { color: '#6b7280', backgroundColor: '#f9fafb', border: '1px solid #f3f4f6' }
    case 'LINK':
      return { color: '#8b5cf6', backgroundColor: '#f5f3ff', border: '1px solid #ddd6fe' }
    default:
      return { color: '#6366f1', backgroundColor: '#eef2ff', border: '1px solid #e0e7ff' }
  }
}


// 客户端过滤
const filteredEntries = computed(() => {
  return entries.value.filter(item => {
    // 1. 过滤掉图片和表格类型，因为它们属于图表/媒体数据，不显示在前台首页词条列表中
    if (item.entryType === 'image' || item.entryType === 'table') return false
    // 2. 关键词/名称检索
    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase()
      const title = (item.title || '').toLowerCase()
      const content = (item.content || '').toLowerCase()
      const keywords = (item.keywords || '').toLowerCase()
      
      const titleMatch = title.includes(q)
      const contentMatch = content.includes(q)
      const kwMatch = keywords.includes(q)
      
      if (!titleMatch && !contentMatch && !kwMatch) return false
    }
    return true
  })
})

// 分页控制
const currentPage = ref(1)
const pageSize = ref(20) // 一页固定20个词条，紧凑列表更高效

const paginatedEntries = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredEntries.value.slice(start, end)
})

// 过滤/搜索变化时自动重置回第一页，并防抖从服务端重新拉取数据
let searchTimeout: any = null
watch(searchQuery, () => {
  currentPage.value = 1
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    loadEntries()
  }, 300)
})


// 详情 & 编辑
const detailVisible = ref(false)
const selectedEntry = ref<KnowledgeEntry | null>(null)
const editMode = ref(false)
const editTableMarkdown = ref('')
const editSaving = ref(false)

function showDetail(item: KnowledgeEntry) {
  selectedEntry.value = item
  editMode.value = false
  detailVisible.value = true
}

function handleDocClick(doc: any) {
  if (!doc) return
  if (doc.url || (doc.title && (doc.title.startsWith('http://') || doc.title.startsWith('https://')))) {
    const targetUrl = doc.url || doc.title
    window.open(targetUrl, '_blank')
  } else {
    window.open(getDocFileUrl(doc.id), '_blank')
  }
}

function renderMarkdownContent(md: string): string {
  if (!md) return ''
  return marked.parse(md) as string
}

function getTypeStyle(type: string) {
  const t = (type || '').toLowerCase()
  switch (t) {
    case 'concept':
      return { label: '概念 (Concept)', icon: 'Notebook', color: '#2563eb', bg: '#eff6ff' }
    case 'entity':
      return { label: '实体 (Entity)', icon: 'OfficeBuilding', color: '#0d9488', bg: '#f0fdfa' }
    case 'thesis':
      return { label: '论点 (Thesis)', icon: 'Opportunity', color: '#7c3aed', bg: '#f5f3ff' }
    case 'methodology':
      return { label: '方法论 (Methodology)', icon: 'Setting', color: '#d97706', bg: '#fffbeb' }
    case 'finding':
      return { label: '发现 (Finding)', icon: 'Search', color: '#0891b2', bg: '#ecfeff' }
    case 'comparison':
      return { label: '对比 (Comparison)', icon: 'Switch', color: '#db2777', bg: '#fdf2f8' }
    case 'synthesis':
      return { label: '综合 (Synthesis)', icon: 'Files', color: '#4f46e5', bg: '#eef2ff' }
    case 'image':
      return { label: '图片 (Image)', icon: 'Picture', color: '#0891b2', bg: '#ecfeff' }
    case 'table':
      return { label: '表格 (Table)', icon: 'Grid', color: '#475569', bg: '#f1f5f9' }
    default:
      return { label: '词条 (Wiki)', icon: 'Document', color: '#4b5563', bg: '#f3f4f6' }
  }
}

function navigateToRelated(relatedTitle: string) {
  if (!relatedTitle) return
  const found = entries.value.find(e => e.title === relatedTitle || e.title.trim().toLowerCase() === relatedTitle.trim().toLowerCase())
  if (found) {
    showDetail(found)
  } else {
    ElMessage.warning(`未找到词条: ${relatedTitle}`)
  }
}

function startEditTable() {
  if (selectedEntry.value) {
    editTableMarkdown.value = selectedEntry.value.tableMarkdown || ''
    editMode.value = true
  }
}

function cancelEditTable() {
  editMode.value = false
}

async function saveTableEdit() {
  if (!selectedEntry.value) return
  editSaving.value = true
  try {
    await updateTableMarkdown(selectedEntry.value.id, editTableMarkdown.value)
    ElMessage.success('表格保存并同步成功')
    
    // 更新本地状态
    selectedEntry.value.tableMarkdown = editTableMarkdown.value
    // 同时更新列表中的条目
    const listEntry = entries.value.find(e => e.id === selectedEntry.value?.id)
    if (listEntry) {
      listEntry.tableMarkdown = editTableMarkdown.value
    }
    editMode.value = false
    // 重新加载词条列表以更新图谱
    await loadEntries()
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
  editSaving.value = false
}

// 知识库文档列表数据
const docList = ref<Record<string, any[]>>({})
const showUpload = ref(false)
const uploading = ref(false)
const files = ref<File[]>([])
const uploadForm = reactive({ categoryL1: '', categoryL2: '', docType: 'report', sourceOrigin: '', sourceDocId: null as number | null, sourcePage: null as number | null, chartType: 'image' })

const uploadProgress = reactive({
  current: 0,
  total: 0,
  percentage: 0,
  step: 0,
  currentFile: '',
  log: [] as string[],
})

const libraries = [
  { key: 'report', name: '研究报告库' },
  { key: 'dynamic', name: '动态信息库' },
  { key: 'translation', name: '译丛译著库' },
  { key: 'chart', name: '图表库' },
]

// 加载各知识库的文档
async function loadDocStats() {
  for (const lib of libraries) {
    try {
      const res = await getDocuments({ docType: lib.key, page: 1, pageSize: 50 })
      docList.value[lib.key] = res.data.items || []
    } catch (e) {
      console.error(e)
    }
  }
}

function handleFileChange(file: any) {
  files.value.push(file.raw)
}

async function submitUpload() {
  if (files.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  uploadProgress.total = files.value.length
  uploadProgress.current = 0
  uploadProgress.log = []

  for (const file of files.value) {
    uploadProgress.current++
    uploadProgress.currentFile = file.name
    uploadProgress.step = 1
    uploadProgress.percentage = Math.round(((uploadProgress.current - 1) / uploadProgress.total) * 100)
    uploadProgress.log.push(`[${uploadProgress.current}/${uploadProgress.total}] 正在上传: ${file.name}`)

    const formData = new FormData()
    formData.append('file', file)
    formData.append('categoryL1', uploadForm.categoryL1)
    formData.append('categoryL2', uploadForm.categoryL2)
    formData.append('docType', uploadForm.docType)
    formData.append('sourceOrigin', uploadForm.sourceOrigin)
    if (uploadForm.sourceDocId) formData.append('sourceDocId', String(uploadForm.sourceDocId))
    if (uploadForm.sourcePage) formData.append('sourcePage', String(uploadForm.sourcePage))
    if (uploadForm.docType === 'chart') {
      formData.append('fileType', uploadForm.chartType)
    }

    try {
      uploadProgress.step = 2
      uploadProgress.log.push(`[${uploadProgress.current}/${uploadProgress.total}] 解析内容: ${file.name}`)

      const res = await uploadFile(formData)
      uploadProgress.step = 3
      const entryCount = res.data.entry_count || 0
      uploadProgress.log.push(`[${uploadProgress.current}/${uploadProgress.total}] LLM抽取完成: ${entryCount} 个词条`)
    } catch (e: any) {
      uploadProgress.log.push(`[${uploadProgress.current}/${uploadProgress.total}] 失败: ${e.message || '未知错误'}`)
      ElMessage.error(`上传失败 (${file.name}): ${e.message || '未知错误'}`)
    }
  }

  uploadProgress.step = 4
  uploadProgress.percentage = 100
  uploadProgress.currentFile = ''
  uploadProgress.log.push('全部处理完成')
  ElMessage.success(`上传完成：共${files.value.length}个文件`)

  uploading.value = false
  files.value = []
  showUpload.value = false
  
  // 刷新列表数据
  await Promise.all([loadDocStats(), loadEntries()])
}

async function loadEntries() {
  loadingEntries.value = true
  try {
    const params: any = { page: 1, pageSize: 250 }
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    const res = await getKnowledgeEntries(params)
    entries.value = res.data.items || []
  } catch (e) {
    console.error('加载词条列表失败', e)
  }
  loadingEntries.value = false
}

onMounted(async () => {
  await Promise.all([loadDocStats(), loadEntries()])
})
</script>

<style scoped>
.home-page { padding: 0px; }
.hero-section {
  text-align: center;
  padding: 35px 0;
  background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 100%);
  border-radius: 8px;
  margin-bottom: 24px;
  border: 1px solid #E2E8F0;
}
.hero-section h1 { font-size: 32px; color: #2F5496; margin-bottom: 8px; font-weight: 700; }
.hero-section p { font-size: 16px; color: #64748B; }

.wiki-entries-section {
  padding: 24px;
  background: #F8FAFC;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
  margin-bottom: 24px;
}
.wiki-section-header {
  margin-bottom: 20px;
}
.wiki-section-header h2 {
  font-size: 20px;
  color: #1E293B;
  font-weight: 700;
  margin: 0 0 4px;
}
.wiki-subtitle {
  font-size: 12px;
  color: #64748B;
}

.filter-toolbar {
  background: white;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.wiki-cards {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 12px;
}
.wiki-list-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.18s ease;
  border: 1px solid transparent;
}
.wiki-list-item-even {
  background: #F8FAFC;
}
.wiki-list-item:hover {
  background: #EFF6FF;
  border-color: #BFDBFE;
  transform: translateX(2px);
  box-shadow: 0 1px 4px rgba(47, 84, 150, 0.08);
}
.wiki-list-item-selected {
  background: #EFF6FF !important;
  border-color: #BFDBFE !important;
}
.wiki-type-icon {
  flex-shrink: 0;
  font-size: 15px;
}
.wiki-list-title {
  flex: 1;
  font-size: 13px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}
.wiki-type-badge {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 7px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}
.wiki-content {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 12px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.wiki-tags {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.wiki-keyword-tag {
  background: #F1F5F9;
  color: #475569;
  border-color: #E2E8F0;
}
.more-tags {
  color: #94A3B8;
  font-size: 12px;
  margin-left: 2px;
}
.wiki-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #F1F5F9;
  padding-top: 8px;
  margin-top: auto;
  font-size: 11px;
}
.wiki-source-doc {
  color: #64748B;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 70%;
}
.wiki-lib-badge {
  flex-shrink: 0;
}

/* 原始图片/表格卡片 */
.original-image-card-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background: #FAFBFC;
  border-radius: 4px;
}
.original-image-preview {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.original-table-card-wrap {
  width: 100%;
  height: 100%;
  overflow: auto;
  font-size: 11px;
  color: #334155;
}

/* 分割线 */
.section-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #CBD5E1, transparent);
  margin: 30px 0;
}

/* 知识库资料列表区域 */
.libraries-section {
  padding: 24px;
  background: white;
  border-radius: 10px;
  border: 1px solid #E2E8F0;
  margin-bottom: 24px;
}
.libraries-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.libraries-title-wrap h2 {
  font-size: 20px;
  color: #1E293B;
  font-weight: 700;
  margin: 0 0 4px;
}
.libraries-subtitle {
  font-size: 12px;
  color: #64748B;
}
.lib-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.lib-row {
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}
.lib-row-header {
  background: linear-gradient(180deg, #F8FAFC, #EFF6FF);
  padding: 12px 18px;
  border-bottom: 1px solid #E2E8F0;
}
.lib-row-name {
  font-size: 14px;
  font-weight: bold;
  color: #1E3A8A;
}
.lib-row-body {
  padding: 16px 20px;
  background: white;
}

/* 资料列表卡片式展示（包含封面） */
.doc-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  width: 100%;
}
.doc-card {
  background: white;
  border-radius: 8px;
  border: 1px solid #E2E8F0;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  height: 240px;
  transition: all 0.2s ease-in-out;
  box-sizing: border-box;
}
.doc-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.doc-cover-wrapper {
  flex: 1;
  background: #F8FAFC;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  border-bottom: 1px solid #E2E8F0;
  height: 170px;
}
.doc-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: top;
}
.placeholder-cover {
  font-size: 36px;
  color: #94A3B8;
}
.doc-info {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  justify-content: center;
  height: 70px;
  box-sizing: border-box;
}
.doc-title-text {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-time-text {
  font-size: 10px;
  color: #94A3B8;
}
.doc-empty {
  font-size: 13px;
  color: #94A3B8;
  text-align: center;
  padding: 8px 0;
}

/* 横向滚动布局 */
.doc-preview.horizontal-scroll {
  display: flex;
  overflow-x: auto;
  gap: 16px;
  padding-bottom: 12px;
  width: 100%;
}
.doc-preview.horizontal-scroll::-webkit-scrollbar {
  height: 6px;
}
.doc-preview.horizontal-scroll::-webkit-scrollbar-thumb {
  background: #CBD5E1;
  border-radius: 3px;
}
.doc-preview.horizontal-scroll::-webkit-scrollbar-track {
  background: transparent;
}
.chart-card {
  flex-shrink: 0;
  width: 220px;
  height: 150px;
  cursor: pointer;
}
.chart-cover {
  height: 100%;
}

/* 列表展示布局 */
.doc-list-view {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.doc-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: white;
  border-radius: 6px;
  border: 1px solid #E2E8F0;
  transition: all 0.2s ease;
  cursor: pointer;
}
.doc-list-item:hover {
  background: #EFF6FF;
  border-color: #BFDBFE;
  transform: translateX(2px);
}
.doc-file-icon {
  font-size: 16px;
  color: #64748B;
}
.doc-title-link {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.doc-type-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
}
.doc-time-label {
  font-size: 11px;
  color: #94A3B8;
}

/* 详情弹窗 */
.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #64748B;
}
.source-document {
  color: #334155;
}
.create-time {
  margin-left: auto;
}
.meta-label {
  font-weight: 600;
  font-size: 13px;
  color: #475569;
  margin-right: 8px;
}
.detail-content-area {
  padding: 8px 4px;
}
.entry-content {
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  margin: 16px 0;
}
.dialog-image-box {
  display: flex;
  justify-content: center;
  background: #FAFBFC;
  border-radius: 8px;
  padding: 12px;
  border: 1px solid #E2E8F0;
}
.detail-image {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
  display: block;
}
.detail-table-wrapper {
  margin-top: 8px;
}
.edit-preview-box {
  margin-top: 8px;
  border: 1px solid #E2E8F0;
  border-radius: 6px;
  padding: 12px;
  max-height: 250px;
  overflow: auto;
  background: white;
}

/* Upload progress */
.upload-progress-area {
  padding: 16px 0;
}
.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 4px;
}
.progress-steps {
  display: flex;
  justify-content: space-between;
  margin: 12px 0;
  padding: 0 8px;
}
.step {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #CBD5E1;
  transition: color 0.3s;
}
.step.active { color: #409EFF; font-weight: 500; }
.step.done { color: #10B981; }
.step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #E2E8F0;
  display: inline-block;
}
.step.active .step-dot { background: #409EFF; }
.step.done .step-dot { background: #10B981; }
.current-file {
  font-size: 12px;
  color: #64748B;
  margin-top: 8px;
  padding: 6px 10px;
  background: #F8FAFC;
  border-radius: 4px;
}
.progress-log {
  max-height: 120px;
  overflow-y: auto;
  margin-top: 8px;
  padding: 8px;
  background: #1E293B;
  border-radius: 6px;
}
.log-line {
  font-size: 11px;
  font-family: 'SF Mono', Menlo, monospace;
  color: #94A3B8;
  line-height: 1.8;
}
.wiki-pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* llm_wiki styled frontmatter panel */
.wiki-frontmatter-panel {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 8px;
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.8), 0 1px 3px rgba(0, 0, 0, 0.02);
}
.fm-identity {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}
.fm-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.fm-title-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.fm-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}
.fm-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 11px;
}
.fm-badge-type {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
}
.fm-meta-item {
  color: #64748b;
  display: flex;
  align-items: center;
}
.fm-meta-tag {
  color: #64748b;
  display: flex;
  align-items: center;
  background: rgba(226, 232, 240, 0.6);
  padding: 1px 5px;
  border-radius: 4px;
}
.fm-description {
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
  font-style: italic;
  margin-top: 8px;
  padding-left: 8px;
  border-left: 3px solid #cbd5e1;
}
.fm-origin {
  font-size: 12px;
  color: #1e293b;
  background: #eff6ff;
  border-left: 3px solid #3b82f6;
  padding: 6px 10px;
  border-radius: 0 6px 6px 0;
  margin-top: 10px;
}
.fm-origin-label {
  font-weight: 600;
  color: #2563eb;
}
.fm-section {
  margin-top: 12px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 8px;
}
.fm-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.fm-section-count {
  color: #94a3b8;
  font-weight: normal;
  margin-left: 4px;
}
.fm-sources-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.fm-source-card {
  display: flex;
  align-items: center;
  gap: 6px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  color: #334155;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}
.fm-source-icon {
  font-size: 14px;
}
.fm-source-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fm-related-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.fm-related-chip {
  font-size: 11px;
  font-weight: 500;
  color: #4f46e5;
  background: #f5f3ff;
  border: 1px solid #e0e7ff;
  padding: 3px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}
.fm-related-chip:hover {
  background: #e0e7ff;
  color: #3730a3;
  transform: translateY(-1px);
}
.wiki-body-content {
  padding: 4px 8px;
}
.image-caption-text {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin-top: 12px;
  text-align: center;
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}
</style>

<style>
/* 全局 markdown 表格样式供 v-html 渲染使用 */
.wiki-detail-dialog .markdown-body table,
.wiki-detail-dialog .edit-preview-box table,
.wiki-cards .markdown-body table {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  font-size: 12px;
}
.wiki-detail-dialog .markdown-body table th,
.wiki-detail-dialog .markdown-body table td,
.wiki-detail-dialog .edit-preview-box table th,
.wiki-detail-dialog .edit-preview-box table td,
.wiki-cards .markdown-body table th,
.wiki-cards .markdown-body table td {
  border: 1px solid #CBD5E1;
  padding: 6px 8px;
  text-align: left;
}
.wiki-detail-dialog .markdown-body table th,
.wiki-detail-dialog .edit-preview-box table th,
.wiki-cards .markdown-body table th {
  background: #F1F5F9;
  font-weight: 600;
  color: #1E293B;
}
.wiki-detail-dialog .markdown-body table tr:nth-child(even),
.wiki-detail-dialog .edit-preview-box table tr:nth-child(even),
.wiki-cards .markdown-body table tr:nth-child(even) {
  background: #F8FAFC;
}
.wiki-detail-dialog .markdown-body table tr:hover td,
.wiki-detail-dialog .edit-preview-box table tr:hover td,
.wiki-cards .markdown-body table tr:hover td {
  background: #EFF6FF;
}
</style>
