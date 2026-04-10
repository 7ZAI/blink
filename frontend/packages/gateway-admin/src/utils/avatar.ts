/**
 * 本地头像资源管理
 * 使用 assets/avatar 目录下的 SVG 文件提供头像选择
 */

// 使用 Vite 的 import.meta.glob 导入所有头像 SVG 文件
const avatarModules = import.meta.glob<{ default: string }>('@/assets/avatar/*.svg', {
  eager: true,
})

/**
 * 头像样式列表
 * 每个样式对应 assets/avatar 目录下的一个 SVG 文件
 */
export const AVATAR_STYLES = [
  // 原始样式
  { value: 'fun-emoji', label: 'Fun Emoji' },
  { value: 'avataaars', label: 'Avataaars' },
  { value: 'avataaars-neutral', label: 'Avataaars Neutral' },
  { value: 'adventurer', label: 'Adventurer' },
  { value: 'adventurer-neutral', label: 'Adventurer Neutral' },
  { value: 'big-ears', label: 'Big Ears' },
  { value: 'big-ears-neutral', label: 'Big Ears Neutral' },
  { value: 'big-smile', label: 'Big Smile' },
  { value: 'bottts', label: 'Bottts' },
  { value: 'bottts-neutral', label: 'Bottts Neutral' },
  { value: 'croodles', label: 'Croodles' },
  { value: 'croodles-neutral', label: 'Croodles Neutral' },
  { value: 'dylan', label: 'Dylan' },
  { value: 'glass', label: 'Glass' },
  { value: 'icons', label: 'Icons' },
  { value: 'identicon', label: 'Identicon' },
  { value: 'initials', label: 'Initials' },
  { value: 'lorelei', label: 'Lorelei' },
  { value: 'lorelei-neutral', label: 'Lorelei Neutral' },
  { value: 'micah', label: 'Micah' },
  { value: 'miniavs', label: 'Miniavs' },
  { value: 'notionists', label: 'Notionists' },
  { value: 'notionists-neutral', label: 'Notionists Neutral' },
  { value: 'open-peeps', label: 'Open Peeps' },
  { value: 'personas', label: 'Personas' },
  { value: 'pixel-art', label: 'Pixel Art' },
  { value: 'pixel-art-neutral', label: 'Pixel Art Neutral' },
  { value: 'rings', label: 'Rings' },
  { value: 'shapes', label: 'Shapes' },
  { value: 'thumbs', label: 'Thumbs' },
  // 变体样式
  { value: 'fun-emoji-2', label: 'Fun Emoji 2' },
  { value: 'avataaars-2', label: 'Avataaars 2' },
  { value: 'avataaars-neutral-2', label: 'Avataaars Neutral 2' },
  { value: 'adventurer-2', label: 'Adventurer 2' },
  { value: 'adventurer-neutral-2', label: 'Adventurer Neutral 2' },
  { value: 'big-ears-2', label: 'Big Ears 2' },
  { value: 'big-ears-neutral-2', label: 'Big Ears Neutral 2' },
  { value: 'big-smile-2', label: 'Big Smile 2' },
  { value: 'bottts-2', label: 'Bottts 2' },
  { value: 'bottts-neutral-2', label: 'Bottts Neutral 2' },
  { value: 'croodles-2', label: 'Croodles 2' },
  { value: 'croodles-neutral-2', label: 'Croodles Neutral 2' },
  { value: 'dylan-2', label: 'Dylan 2' },
  { value: 'glass-2', label: 'Glass 2' },
  { value: 'icons-2', label: 'Icons 2' },
  { value: 'identicon-2', label: 'Identicon 2' },
  { value: 'initials-2', label: 'Initials 2' },
  { value: 'lorelei-2', label: 'Lorelei 2' },
  { value: 'lorelei-neutral-2', label: 'Lorelei Neutral 2' },
  { value: 'micah-2', label: 'Micah 2' },
  { value: 'miniavs-2', label: 'Miniavs 2' },
  { value: 'notionists-2', label: 'Notionists 2' },
  { value: 'notionists-neutral-2', label: 'Notionists Neutral 2' },
  { value: 'open-peeps-2', label: 'Open Peeps 2' },
  { value: 'personas-2', label: 'Personas 2' },
  { value: 'pixel-art-2', label: 'Pixel Art 2' },
  { value: 'pixel-art-neutral-2', label: 'Pixel Art Neutral 2' },
  { value: 'rings-2', label: 'Rings 2' },
  { value: 'shapes-2', label: 'Shapes 2' },
  { value: 'thumbs-2', label: 'Thumbs 2' },
] as const

export type AvatarStyle = (typeof AVATAR_STYLES)[number]['value']

/**
 * 头像 URL 缓存映射
 */
const avatarUrlMap = new Map<string, string>()

/**
 * 初始化头像 URL 映射
 * 将导入的 SVG 模块路径映射为头像名称
 */
Object.entries(avatarModules).forEach(([path, module]) => {
  // 从路径中提取头像名称，如 /src/assets/avatar/fun-emoji.svg -> fun-emoji
  const match = path.match(/\/([^/]+)\.svg$/)
  const avatarName = match?.[1]
  if (avatarName && module?.default) {
    avatarUrlMap.set(avatarName, module.default)
  }
})

/**
 * 获取本地头像 URL
 *
 * @param avatarName 头像名称（如 'fun-emoji'）
 * @returns 头像 URL，如果未找到则返回默认头像
 */
export const getLocalAvatarUrl = (avatarName: string | undefined | null): string => {
  if (!avatarName) {
    // 返回默认头像
    return avatarUrlMap.get('fun-emoji') || ''
  }

  // 如果是完整的 HTTP URL，直接返回
  if (avatarName.startsWith('http')) {
    return avatarName
  }

  // 从映射中获取本地头像 URL
  return avatarUrlMap.get(avatarName) || avatarUrlMap.get('fun-emoji') || ''
}

/**
 * 获取头像 URL（兼容旧接口）
 * 优先使用本地头像，如果是外部 URL 则直接返回
 *
 * @param avatar 头像名称或 URL
 * @param _avatarStyle 废弃参数，保留用于兼容
 * @param _seed 废弃参数，保留用于兼容
 * @returns 头像 URL
 */
export const getAvatarUrl = (
  avatar: string | undefined | null,
  _avatarStyle?: string | null,
  _seed?: string
): string => {
  return getLocalAvatarUrl(avatar)
}

/**
 * 获取所有可用的头像名称列表
 *
 * @returns 头像名称数组
 */
export const getAvailableAvatars = (): string[] => {
  return AVATAR_STYLES.map((style) => style.value)
}
