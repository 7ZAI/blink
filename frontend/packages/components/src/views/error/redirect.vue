<template>
  <div></div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  // 支持两种格式:
  // 1. /redirect/path/to/page (path参数方式)
  // 2. /redirect?redirect=/path/to/page (query参数方式)
  const redirectPath = route.params.path
    ? '/' + (Array.isArray(route.params.path) ? route.params.path.join('/') : route.params.path)
    : route.query.redirect as string

  if (redirectPath) {
    router.replace(redirectPath)
  } else {
    router.replace('/')
  }
})
</script>