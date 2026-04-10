import { ref, computed } from 'vue'
import { getChannelList } from '@/api/channel'
import { getRouteList } from '@/api/route'

export interface SearchResult {
  type: 'channel' | 'route' | 'config'
  id: string
  name: string
  path: string
}

export const useSearch = () => {
  const channels = ref<any[]>([])
  const routes = ref<any[]>([])
  const searchQuery = ref('')

  const loadSearchData = async () => {
    try {
      const [channelRes, routeRes] = await Promise.all([
        getChannelList({ pageNum: 1, pageSize: 100 }),
        getRouteList({ pageNum: 1, pageSize: 100 }),
      ])
      channels.value = (channelRes as any)?.rows || []
      routes.value = (routeRes as any)?.rows || []
    } catch (error) {
      console.error('Failed to load search data:', error)
    }
  }

  const searchResults = computed<SearchResult[]>(() => {
    if (!searchQuery.value.trim()) return []

    const query = searchQuery.value.toLowerCase()
    const results: SearchResult[] = []

    // Search channels
    channels.value.forEach((channel) => {
      const name = (channel.channelName || channel.channelId || '').toLowerCase()
      const id = (channel.channelId || '').toLowerCase()
      if (name.includes(query) || id.includes(query)) {
        results.push({
          type: 'channel',
          id: channel.channelId,
          name: channel.channelName || channel.channelId,
          path: `/channel`,
        })
      }
    })

    // Search routes
    routes.value.forEach((route) => {
      const name = (route.routeName || route.id || '').toLowerCase()
      const id = (route.id || '').toLowerCase()
      if (name.includes(query) || id.includes(query)) {
        results.push({
          type: 'route',
          id: route.id,
          name: route.routeName || route.id,
          path: `/route`,
        })
      }
    })

    return results.slice(0, 10)
  })

  return {
    searchQuery,
    searchResults,
    loadSearchData,
  }
}
