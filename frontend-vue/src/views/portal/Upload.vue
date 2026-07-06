<template>
  <div class="upload-page">
    <el-card>
      <template #header><span>资料上传</span></template>
      <el-form :model="form" label-width="120px">
        <el-form-item label="目标库">
          <el-select v-model="form.library" placeholder="选择目标库" style="width: 100%">
            <el-option label="研究报告库" value="report" />
            <el-option label="动态信息库" value="dynamic" />
            <el-option label="译丛译著库" value="translation" />
            <el-option label="图表数据库" value="chart" />
            <el-option label="政策文件库" value="policy" />
            <el-option label="新闻资讯库" value="news" />
          </el-select>
        </el-form-item>
        <el-form-item label="一级分类">
          <el-select v-model="form.categoryL1" placeholder="选择分类" clearable style="width: 100%">
            <el-option label="政治" value="政治" />
            <el-option label="军事" value="军事" />
            <el-option label="经济" value="经济" />
            <el-option label="科技" value="科技" />
            <el-option label="社会" value="社会" />
            <el-option label="文化" value="文化" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="资料类型">
          <el-select v-model="form.docType" placeholder="选择类型" style="width: 100%">
            <el-option label="研究报告" value="report" />
            <el-option label="动态信息" value="dynamic" />
            <el-option label="译丛译著" value="translation" />
            <el-option label="图表数据" value="chart" />
            <el-option label="政策文件" value="policy" />
            <el-option label="新闻资讯" value="news" />
          </el-select>
        </el-form-item>
        <el-form-item label="AI智能分类">
          <el-switch v-model="form.autoClassify" />
          <span style="margin-left: 12px; color: #909399; font-size: 12px;">
            启用后AI将自动分析文档内容并推荐分类
          </span>
        </el-form-item>
        <el-form-item label="来源信息">
          <el-input v-model="form.sourceOrigin" placeholder="可选：填写来源信息，如论文名、Blog URL、报告出处等" clearable />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            若来自论文或Blog，请注明出处。留空则系统自动从文件名提取。
          </div>
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :limit="10"
            multiple
            drag
            :accept="acceptTypes"
          >
            <el-icon style="font-size: 48px; color: #c0c4cc;"><UploadFilled /></el-icon>
            <div>拖拽文件到此处或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持格式：PDF、Word(.doc/.docx)、WPS文字(.wps)、Excel(.xls/.xlsx)、WPS表格(.et)、PPT(.ppt/.pptx)、WPS演示(.dps)、TXT、Markdown、图片（JPG/PNG/GIF/TIFF/WebP/SVG等），单个文件不超过100MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitUpload" :loading="uploading">开始上传</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
      <el-divider />
      <div v-if="results.length > 0">
        <h4>上传结果</h4>
        <el-table :data="results" stripe style="width: 100%">
          <el-table-column prop="filename" label="文件名" width="200" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="library" label="目标库" width="120" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column prop="entry_count" label="词条数" width="80" />
          <el-table-column prop="message" label="信息" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadFile } from '../../api'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'

const form = reactive({
  categoryL1: '',
  categoryL2: '',
  docType: 'report',
  library: 'report',
  autoClassify: false,
  sourceOrigin: ''
})

// 自动同步 docType 和 library（用户选图表库时docType也设为chart）
watch(() => form.library, (val) => { form.docType = val })

const files = ref<File[]>([])
const uploading = ref(false)
const results = ref<any[]>([])

// 支持的文件类型（含WPS格式和更多图片格式）
const acceptTypes = '.pdf,.doc,.docx,.wps,.xls,.xlsx,.et,.ppt,.pptx,.dps,.odt,.ods,.odp,.txt,.md,.markdown,.rtf,.csv,.jpg,.jpeg,.png,.gif,.bmp,.webp,.tiff,.tif,.svg,.ico,.heic,.heif,.avif'

function handleFileChange(file: UploadFile) {
  if (file.raw) {
    files.value.push(file.raw)
  }
}

function handleFileRemove(file: UploadFile) {
  const index = files.value.findIndex(f => f.name === file.name)
  if (index > -1) {
    files.value.splice(index, 1)
  }
}

function getStatusType(status: string) {
  switch (status) {
    case 'success': return 'success'
    case 'skipped': return 'warning'
    case 'error': return 'danger'
    default: return 'info'
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'success': return '成功'
    case 'skipped': return '跳过'
    case 'error': return '失败'
    default: return status
  }
}

async function submitUpload() {
  if (files.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  results.value = []

  // 使用批量上传API
  const formData = new FormData()
  files.value.forEach(file => {
    formData.append('files', file)
  })
  formData.append('categoryL1', form.categoryL1)
  formData.append('categoryL2', form.categoryL2)
  formData.append('docType', form.docType)
  formData.append('library', form.library)
  formData.append('autoClassify', String(form.autoClassify))
  formData.append('sourceOrigin', form.sourceOrigin)

  try {
    const res = await uploadFile(formData, '/api/upload/batch')
    results.value = res.data.results || []
    ElMessage.success(`上传完成：共${res.data.total}个文件`)
  } catch (e: any) {
    ElMessage.error('上传失败: ' + e.message)
  }

  uploading.value = false
}

function resetForm() {
  files.value = []
  results.value = []
  form.categoryL1 = ''
  form.categoryL2 = ''
  form.docType = 'report'
  form.library = 'report'
  form.autoClassify = false
}
</script>

<style scoped>
.upload-page { padding: 20px; max-width: 900px; margin: 0 auto; }
.el-upload__tip { margin-top: 8px; color: #909399; font-size: 12px; }
</style>
