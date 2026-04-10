import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface GroupInfo {
  groupId: number
  groupNo: string
  groupName: string
  groupEnName: string
  groupParentId: number
  groupLevel: number
  isLeaf: number
  groupLeader: string
  groupAddress: string
  phone: string
  createTime: string
  createBy: string
  updateTime: string
  updateBy: string
  children?: GroupInfo[]
}

export interface QueryGroupParams {
  groupName?: string
  groupParentId?: number
}

export interface QueryGroupRsp {
  list: GroupInfo[]
}

export interface AddGroupParams {
  groupName: string
  groupEnName?: string
  groupNo?: string
  groupParentId?: number
  groupLeader?: string
  groupAddress?: string
  phone?: string
  isLeaf?: number
}

export interface UpdateGroupParams {
  groupId: number
  groupName?: string
  groupEnName?: string
  groupNo?: string
  groupParentId?: number
  groupLeader?: string
  groupAddress?: string
  phone?: string
  isLeaf?: number
}

export interface DeleteGroupParams {
  deleteId?: number
  idList?: number[]
  batchDelete: boolean
}

export const getGroupList = (params?: QueryGroupParams): Promise<QueryGroupRsp> => {
  return request.post('/sysGroup/getSysGroupList', { body: params || {} }) as Promise<QueryGroupRsp>
}

function buildGroupTree(groups: GroupInfo[]): GroupInfo[] {
  if (!groups || groups.length === 0) {
    return []
  }

  const groupMap = new Map<number, GroupInfo>()
  const rootGroups: GroupInfo[] = []

  // 第一遍遍历：创建所有节点
  groups.forEach((group) => {
    groupMap.set(group.groupId, { ...group, children: [] })
  })

  // 第二遍遍历：建立父子关系
  groups.forEach((group) => {
    const currentGroup = groupMap.get(group.groupId)!
    if (!group.groupParentId || group.groupParentId === 0) {
      // 根节点
      rootGroups.push(currentGroup)
    } else {
      const parentGroup = groupMap.get(group.groupParentId)
      if (parentGroup) {
        if (!parentGroup.children) {
          parentGroup.children = []
        }
        parentGroup.children.push(currentGroup)
      } else {
        // 父节点不存在时，将该节点作为根节点展示
        rootGroups.push(currentGroup)
      }
    }
  })

  // 按 groupLevel 排序
  const sortGroups = (list: GroupInfo[]): GroupInfo[] => {
    list.sort((a, b) => (a.groupLevel || 0) - (b.groupLevel || 0))
    list.forEach((group) => {
      if (group.children && group.children.length > 0) {
        sortGroups(group.children)
      }
    })
    return list
  }

  return sortGroups(rootGroups)
}

export const getGroupTree = async (): Promise<GroupInfo[]> => {
  const res = await getGroupList()
  return buildGroupTree(res.list || [])
}

export const addGroup = (params: AddGroupParams): Promise<GroupInfo> => {
  return request.post('/sysGroup/saveSysGroup', { body: params }) as Promise<GroupInfo>
}

export const updateGroup = (params: UpdateGroupParams): Promise<GroupInfo> => {
  return request.post('/sysGroup/modifySysGroup', { body: params }) as Promise<GroupInfo>
}

export const deleteGroup = (params: DeleteGroupParams): Promise<void> => {
  return request.post('/sysGroup/deleteSysGroup', { body: params }) as Promise<void>
}
