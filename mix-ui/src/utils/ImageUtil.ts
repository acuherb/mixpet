/**
 * 图片压缩工具：canvas 缩放 + 转 WebP
 * WebP 相比 JPEG 可在相同质量下再省 30%~50% 体积；
 * 分辨率提升到 1280px 兼顾清晰度与体积（原 512px 缩小后在电脑上看会模糊）
 */

/**
 * 压缩图片文件并转成 WebP；已是小尺寸 WebP 才原样返回
 * @param file    原始文件（JPG/PNG 等）
 * @param maxSide 最长边像素上限，默认 1280
 * @param quality WebP 质量 0-1，默认 0.85
 * @returns 压缩后的 WebP File（失败时返回原文件）
 */
export const compressImage = async (file: File, maxSide = 1280, quality = 0.85): Promise<File> => {
  try {
    // 本身已是 WebP 且尺寸没超标、体积合理：直接返回，避免二次编码画质损失
    if (file.type === 'image/webp' && file.size < 200 * 1024) return file
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)
    const ctx = canvas.getContext('2d')
    ctx?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    const blob = await new Promise<Blob | null>(r => canvas.toBlob(r, 'image/webp', quality))
    if (!blob) return file
    return new File([blob], 'photo.webp', {type: blob.type || 'image/webp'})
  } catch (e) {
    return file // 压缩失败就传原图，交给后端处理
  }
}