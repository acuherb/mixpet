/**
 * 图片压缩工具：canvas 缩放 + 转 JPEG
 * 手机拍照动辄 3MB+，压到最大边 512px / 质量 85% 后约 50KB
 */

/**
 * 压缩图片文件；已够小或尺寸不超标的原样返回
 * @param file    原始文件（JPG/PNG）
 * @param maxSide 最长边像素上限，默认 512
 * @param quality JPEG 质量 0-1，默认 0.85
 * @returns 压缩后的 File（失败时返回原文件）
 */
export const compressImage = async (file: File, maxSide = 512, quality = 0.85): Promise<File> => {
  try {
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height))
    // 已经够小且尺寸不超标的直接上传
    if (scale >= 1 && file.size < 400 * 1024) return file
    const canvas = document.createElement('canvas')
    canvas.width = Math.round(bitmap.width * scale)
    canvas.height = Math.round(bitmap.height * scale)
    canvas.getContext('2d')?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    const blob = await new Promise<Blob | null>(r => canvas.toBlob(r, 'image/jpeg', quality))
    if (!blob) return file
    return new File([blob], 'photo.jpg', {type: 'image/jpeg'})
  } catch (e) {
    return file // 压缩失败就传原图，交给后端处理
  }
}
