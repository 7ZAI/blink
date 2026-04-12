# Nacos Config Manager Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a skill for managing Nacos configurations via HTTP API and nacos-cli.

**Architecture:** Single-file skill in `~/.claude/skills/nacos-config-manager/SKILL.md` with HTTP API as primary method, nacos-cli as supplement for profile init and AI extensions.

**Tech Stack:** Nacos HTTP API, nacos-cli tool, shell commands (curl)

---

## File Structure

```
~/.claude/skills/
  nacos-config-manager/
    SKILL.md              # Main skill file (~300 lines)
```

---

### Task 1: Create Skill Directory and SKILL.md

**Files:**
- Create: `~/.claude/skills/nacos-config-manager/SKILL.md`

- [ ] **Step 1: Create skill directory**

```bash
mkdir -p ~/.claude/skills/nacos-config-manager
```

- [ ] **Step 2: Write SKILL.md with frontmatter and overview**

Write file `~/.claude/skills/nacos-config-manager/SKILL.md`:

```markdown
---
name: nacos-config-manager
description: Use when managing Nacos configurations - includes config get/set/list via HTTP API, profile setup via nacos-cli, and environment switching
allowed-tools: Bash(curl:*) Bash(nacos-cli:*)
---

# Nacos Config Manager

## Overview

Manage Nacos configurations efficiently with **HTTP API as primary method** (more stable) and **nacos-cli as supplement** (for profile init and AI extensions).

**Core principle:** HTTP API first, nacos-cli when needed.

## When to Use

- Querying or modifying Nacos service configurations
- Setting up nacos-cli profile for local development
- Managing agentspec or skill resources (nacos-cli only)
- Switching between Nacos environments

## Quick Reference

| Operation | Recommended Method | Reason |
|-----------|-------------------|--------|
| config-get | HTTP API | Stable, no dependency issues |
| config-set | HTTP API | Direct, easy to debug |
| config-list | HTTP API | Flexible filtering |
| config-delete | HTTP API | Simple and reliable |
| profile init | nacos-cli | Local config management |
| agentspec/skill | nacos-cli | HTTP API doesn't support |

## HTTP API Commands

### config-get (获取配置)

```bash
# Basic query
curl "http://${host}:${port}/nacos/v1/cs/configs?dataId=${dataId}&group=${group}&tenant=${namespace}"

# Example: Get gateway-admin config
curl "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=gateway-admin.yaml&group=DEFAULT_GROUP&tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb"
```

| Parameter | Description | Example |
|-----------|-------------|---------|
| host | Nacos server address | `127.0.0.1` |
| port | Nacos server port | `8848` |
| dataId | Config file name | `gateway-admin.yaml` |
| group | Config group | `DEFAULT_GROUP` |
| tenant | Namespace ID (empty = public) | `94984ad7-...` |

### config-set (发布配置)

```bash
# Publish from file
curl -X POST "http://${host}:${port}/nacos/v1/cs/configs" \
  -d "dataId=${dataId}" \
  -d "group=${group}" \
  -d "tenant=${namespace}" \
  --data-urlencode "content@${file}"

# Publish from string
curl -X POST "http://${host}:${port}/nacos/v1/cs/configs" \
  -d "dataId=${dataId}" \
  -d "group=${group}" \
  -d "tenant=${namespace}" \
  --data-urlencode "content=${yamlContent}"
```

### config-list (列出配置)

```bash
# Exact match
curl "http://${host}:${port}/nacos/v1/cs/configs?search=accurate&pageNo=1&pageSize=100&dataId=${dataId}&group=${group}&tenant=${namespace}"

# Fuzzy search (supports * wildcard)
curl "http://${host}:${port}/nacos/v1/cs/configs?search=blur&pageNo=1&pageSize=100&dataId=*&group=DEFAULT_GROUP&tenant=${namespace}"

# List all configs in namespace
curl "http://127.0.0.1:8848/nacos/v1/cs/configs?search=blur&pageNo=1&pageSize=100&tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb"
```

### config-delete (删除配置)

```bash
curl -X DELETE "http://${host}:${port}/nacos/v1/cs/configs?dataId=${dataId}&group=${group}&tenant=${namespace}"
```

## nacos-cli Commands

### Profile 初始化

```bash
# Edit/create profile config
nacos-cli profile edit

# Show current profile
nacos-cli profile show
```

Profile file content (`~/.nacos-cli/default.conf`):

```
host=127.0.0.1
port=8848
namespace=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb
# username=nacos      # Uncomment if auth required
# password=nacos
```

### 配置管理（备用方式）

```bash
# Get config
nacos-cli config-get ${dataId} ${group} \
  --host ${host} --port ${port} --namespace ${namespace}

# Set config from file
nacos-cli config-set ${dataId} ${group} \
  --file ${filepath} \
  --host ${host} --port ${port} --namespace ${namespace}

# List configs
nacos-cli config-list \
  --host ${host} --port ${port} --namespace ${namespace}
```

### AI Agent Extensions (nacos-cli only)

```bash
# agentspec management
nacos-cli agentspec-list --host ${host} --namespace ${namespace}
nacos-cli agentspec-get ${name} --output ~/.agentspecs
nacos-cli agentspec-publish ${zipfile}

# skill management
nacos-cli skill-list --host ${host} --namespace ${namespace}
nacos-cli skill-get ${skillName} --output ~/.skills
nacos-cli skill-publish ${zipfile}
```

## Common Scenarios

### 获取微服务配置

```bash
# Get gateway-admin config
curl "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=gateway-admin.yaml&group=DEFAULT_GROUP&tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb"

# Get blink-base config
curl "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=blink-base.yaml&group=DEFAULT_GROUP&tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb"
```

### 发布新配置

```bash
# From local file
curl -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs" \
  -d "dataId=new-service.yaml" \
  -d "group=DEFAULT_GROUP" \
  -d "tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb" \
  --data-urlencode "content@./new-service.yaml"
```

### 查看所有配置

```bash
curl "http://127.0.0.1:8848/nacos/v1/cs/configs?search=blur&pageNo=1&pageSize=100&tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb"
```

### 初始化 profile

```bash
nacos-cli profile edit
# Edit file with:
# host=127.0.0.1
# port=8848
# namespace=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb
```

## Common Mistakes

### 1. tenant 空值处理

```bash
# Wrong - tenant= means empty string, not omitted
curl "...&tenant="

# Correct - omit tenant for public namespace
curl "...&dataId=app.yaml&group=DEFAULT_GROUP"
```

### 2. URL 编码遗漏

```bash
# Wrong - special chars break the request
curl -X POST ... -d "content=${yaml}"

# Correct - use --data-urlencode
curl -X POST ... --data-urlencode "content=${yaml}"
```

### 3. 认证参数缺失

If Nacos requires auth, add accessToken:

```bash
# Login first
curl -X POST "http://${host}:${port}/nacos/v1/auth/login" \
  -d "username=${username}" -d "password=${password}"

# Use accessToken in subsequent requests
curl "...&accessToken=${token}"
```

## Environment Variables

Store common values for reuse:

```bash
export NACOS_HOST=127.0.0.1
export NACOS_PORT=8848
export NACOS_NS=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb

# Use in commands
curl "http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/cs/configs?dataId=app.yaml&group=DEFAULT_GROUP&tenant=${NACOS_NS}"
```

## Verification

After operations, verify results:

```bash
# Verify config published correctly
curl "http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/cs/configs?dataId=${dataId}&group=DEFAULT_GROUP&tenant=${NACOS_NS}"

# Check config appears in list
curl "http://${NACOS_HOST}:${NACOS_PORT}/nacos/v1/cs/configs?search=accurate&dataId=${dataId}&tenant=${NACOS_NS}"
```
```

- [ ] **Step 3: Verify skill file created**

```bash
ls -la ~/.claude/skills/nacos-config-manager/
wc -l ~/.claude/skills/nacos-config-manager/SKILL.md
```

Expected: SKILL.md exists with ~200-300 lines

---

### Task 2: Test Skill with Real Nacos Operations

**Files:**
- None (verification only)

- [ ] **Step 1: Test HTTP API config-get**

```bash
curl "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=gateway-admin.yaml&group=DEFAULT_GROUP&tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb"
```

Expected: Returns YAML config content for gateway-admin

- [ ] **Step 2: Test HTTP API config-list**

```bash
curl "http://127.0.0.1:8848/nacos/v1/cs/configs?search=blur&pageNo=1&pageSize=10&tenant=94984ad7-b510-4ca4-bdcb-b6cdbd437dfb"
```

Expected: Returns JSON with pageCount and pageItems

- [ ] **Step 3: Verify skill directory structure**

```bash
ls ~/.claude/skills/
```

Expected: Shows `nacos-config-manager` alongside `playwright-cli`

---

### Task 3: Commit Plan and Spec

**Files:**
- Existing: `docs/superpowers/specs/2026-04-12-nacos-config-manager-design.md`
- Existing: `docs/superpowers/plans/2026-04-12-nacos-config-manager.md`

- [ ] **Step 1: Commit spec and plan documents**

```bash
cd /home/binblink/project/blink
git add docs/superpowers/specs/2026-04-12-nacos-config-manager-design.md
git add docs/superpowers/plans/2026-04-12-nacos-config-manager.md
git commit -m "docs: add nacos-config-manager skill design and implementation plan"
```

- [ ] **Step 2: Verify commit**

```bash
git log -1 --oneline
```

Expected: Shows new commit with the docs message

---

## Spec Coverage Check

| Spec Requirement | Task Covered |
|------------------|--------------|
| HTTP API config-get | Task 1, Task 2 |
| HTTP API config-set | Task 1 |
| HTTP API config-list | Task 1, Task 2 |
| HTTP API config-delete | Task 1 |
| nacos-cli profile init | Task 1 |
| nacos-cli agentspec/skill | Task 1 |
| Common scenarios | Task 1 |
| Common mistakes | Task 1 |
| Verification tests | Task 2 |

---

## Placeholder Check

- ✅ No TBD/TODO
- ✅ All code blocks have actual content
- ✅ All commands are executable
- ✅ No "similar to Task X" references