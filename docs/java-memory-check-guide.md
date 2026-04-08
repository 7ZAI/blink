# Java 进程内存检查指南

本文档介绍如何检查 Java 进程的内存占用情况。

## 1. 查找目标 Java 进程

### 方法一: jps 命令

`jps` 是 JDK 提供的工具，专门用于查看 Java 进程：

```bash
jps -l
```

输出示例：
```
10568 com.blink.GatewayAdminApplication
12345 org.gradle.wrapper.GradleWrapperMain
```

- `-l` 参数显示完整的 main class 名称
- 第一列是 PID (进程 ID)

### 方法二: ps 命令

```bash
ps aux | grep -E "gateway.*admin|blink-gateway" | grep -v grep
```

- `ps aux` 显示所有进程详情
- `grep` 过滤目标进程名称
- `grep -v grep` 排除 grep 命令本身

### 方法三: pgrep 命令

```bash
pgrep -f "GatewayAdminApplication"
```

- `-f` 匹配完整命令行

## 2. 查看进程物理内存占用

### ps 命令查看 RSS/VSZ

```bash
ps -p <PID> -o pid,rss,vsz,%mem,comm --no-headers
```

输出示例：
```
10568 640040 8576448  6.2 java
```

字段含义：

| 字段 | 说明 | 示例值 |
|------|------|--------|
| PID | 进程 ID | 10568 |
| RSS | 物理内存占用 (KB) | 640040 KB ≈ 640 MB |
| VSZ | 虚拟内存大小 (KB) | 8576448 KB ≈ 8.3 GB |
| %mem | 内存占用百分比 | 6.2% |
| comm | 进程名称 | java |

**重要概念**：
- **RSS (Resident Set Size)**: 实际占用的物理内存，这是我们需要关注的值
- **VSZ (Virtual Size)**: 虚拟内存大小，包括未实际分配的内存，通常远大于 RSS

## 3. 查看 JVM 堆内存使用

### jstat 命令

```bash
jstat -gc <PID>
```

输出示例：
```
S0C    S1C    S0U    S1U    EC      EU       OC        OU       MC        MU       CCSC     CCSU    YGC   YGCT    FGC   FGCT    CGC    CGCT    GCT
0.0    2048.0 0.0    2048.0 122880.0 94208.0 139264.0  100337.0 109632.0  108641.5 15040.0  14590.3 32    0.183   0     0.000   8      0.011   0.194
```

字段含义：

| 字段 | 说明 |
|------|------|
| S0C/S1C | Survivor 0/1 区容量 (KB) |
| S0U/S1U | Survivor 0/1 区已用 (KB) |
| EC/EU | Eden 区容量/已用 (KB) |
| OC/OU | Old 区容量/已用 (KB) |
| MC/MU | Metaspace 容量/已用 (KB) |
| CCSC/CCSU | Compressed Class Space 容量/已用 (KB) |
| YGC/YGCT | Young GC 次数/耗时 (秒) |
| FGC/FGCT | Full GC 次数/耗时 (秒) |
| CGC/CGCT | 并发 GC 次数/耗时 (秒) |

### jcmd 命令

```bash
jcmd <PID> GC.heap_info
```

输出示例：
```
garbage-first heap   total 264192K, used 201176K
  region size 2048K, 50 young (102400K), 1 survivors (2048K)
Metaspace       used 110569K, committed 111680K, reserved 1179648K
class space    used 14780K, committed 15296K, reserved 1048576K
```

字段含义：
- **total**: 堆总容量
- **used**: 已使用大小
- **committed**: 已提交给 JVM 的内存 (实际分配)
- **reserved**: 预留的内存上限

### jcmd native memory (需启用 NMT)

如果启动时添加了 `-XX:NativeMemoryTracking=summary` 参数：

```bash
jcmd <PID> VM.native_memory summary
```

可以查看更详细的 native memory 分布，包括：
- Java Heap
- Class (Metaspace)
- Thread (线程栈)
- Code (编译代码)
- GC
- Internal

## 4. 内存计算公式

### JVM 总内存 = 堆内存 + 非堆内存

```
总内存 ≈ Heap + Metaspace + Thread Stacks + Code Cache + GC + Direct Memory + Native
```

### 常用内存区域大小估算

| 区域 | 默认值 | 建议配置 |
|------|--------|----------|
| Heap | 物理内存 1/4 | `-Xms512m -Xmx2g` |
| Metaspace | 无上限 | `-XX:MaxMetaspaceSize=256m` |
| Thread Stack | 1MB/线程 | `-Xss256k` |
| Code Cache | 240MB | `-XX:ReservedCodeCacheSize=128m` |
| Direct Memory | 无上限 | `-XX:MaxDirectMemorySize=512m` |

## 5. 完整检查脚本

```bash
#!/bin/bash
# check_java_memory.sh

PID=$1

if [ -z "$PID" ]; then
    echo "Usage: $0 <PID>"
    echo "Available Java processes:"
    jps -l
    exit 1
fi

echo "=== 进程基本信息 ==="
ps -p $PID -o pid,rss,vsz,%mem,comm --no-headers | awk '{
    printf "PID: %s\n", $1
    printf "物理内存 (RSS): %.2f MB\n", $2/1024
    printf "虚拟内存 (VSZ): %.2f MB\n", $3/1024
    printf "内存占比: %s%%\n", $4
}'

echo ""
echo "=== JVM 堆内存 ==="
jcmd $PID GC.heap_info 2>/dev/null | grep -E "heap|Metaspace|class space"

echo ""
echo "=== GC 统计 ==="
jstat -gc $PID 2>/dev/null | awk '{
    if (NR==1) {
        print "区域容量(KB): Eden=" $5 ", Old=" $7 ", Metaspace=" $9
    } else {
        print "区域使用(KB): Eden=" $6 ", Old=" $8 ", Metaspace=" $10
        print "GC次数: Young=" $13 ", Full=" $15 ", Concurrent=" $17
        print "GC耗时(s): Young=" $14 ", Full=" $16 ", Concurrent=" $18 ", Total=" $19
    }
}'
```

使用方法：
```bash
chmod +x check_java_memory.sh
./check_java_memory.sh 10568
```

## 6. 常见问题排查

### 内存占用过高

1. 检查堆内存使用：`jstat -gcutil <PID>` (显示百分比)
2. 检查是否有内存泄漏：多次执行 `jcmd <PID> GC.heap_info` 对比 OU 增长
3. 生成堆转储分析：`jcmd <PID> GC.heap_dump /tmp/heap.hprof`

### GC 频繁

1. 查看 GC 频率：`jstat -gc <PID> 1000` (每秒刷新)
2. 检查 Survivor 区是否不足
3. 调整堆大小或 GC 算法

### Metaspace 持续增长

1. 检查是否有大量动态类加载
2. 设置 Metaspace 上限：`-XX:MaxMetaspaceSize=512m`

## 7. 相关工具汇总

| 工具 | 用途 | 命令示例 |
|------|------|----------|
| jps | 查找 Java 进程 | `jps -l` |
| ps | 查看物理内存 | `ps -p <PID> -o rss,vsz` |
| jstat | GC 和堆统计 | `jstat -gc <PID>` |
| jcmd | 多功能诊断 | `jcmd <PID> GC.heap_info` |
| jmap | 堆转储 | `jmap -dump:format=b,file=heap.hprof <PID>` |
| jvisualvm | GUI 监控 | `jvisualvm` |
| arthas | 阿里开源诊断工具 | `java -jar arthas-boot.jar` |

---

*文档创建时间: 2026-04-08*