# JMeter压力测试

## 1. 安装JMeter

### 1.1 下载

```bash
# 下载JMeter
wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
```

### 1.2 目录结构

```
apache-jmeter-5.6/
├── bin/          # 可执行脚本
├── lib/          # 依赖库
├── docs/         # 文档
└── extras/       # 附加功能
```

## 2. 测试脚本配置

### 2.1 秒杀接口测试

```xml
<!-- seckill-test.jmx -->
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">1000</stringProp>
  <stringProp name="ThreadGroup.ramp_time">60</stringProp>
  <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>

<HTTPSamplerProxy>
  <stringProp name="HTTPSampler.domain">localhost</stringProp>
  <stringProp name="HTTPSampler.port">8080</stringProp>
  <stringProp name="HTTPSampler.path">/api/seckill/seckill</stringProp>
  <stringProp name="HTTPSampler.method">POST</stringProp>
  <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
  <stringProp name="HTTPSampler.body">
    {"activityId":1,"productId":1,"quantity":1}
  </stringProp>
</HTTPSamplerProxy>
```

## 3. 关键配置

### 3.1 线程组配置

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| 线程数 | 并发用户数 | 100-5000 |
| Ramp-up时间 | 启动时间(秒) | 60-300 |
| 持续时间 | 测试时长(秒) | 300+ |
| 延迟创建 | 延迟启动线程 | 不勾选 |

### 3.2 响应断言

```xml
<ResponseAssertion>
  <stringProp name="Assertion.test_field">ResponseCode</stringProp>
  <stringProp name="test_strings">200</stringProp>
</ResponseAssertion>

<ResponseAssertion>
  <stringProp name="Assertion.test_field">ResponseBody</stringProp>
  <stringProp name="test_strings">"code":200</stringProp>
</ResponseAssertion>
```

### 3.3 聚合报告配置

```xml
<Summariser>
  <stringProp name="summariser.name">summary</stringProp>
</Summariser>
```

## 4. 执行测试

```bash
# GUI模式
./bin/jmeter.sh -t seckill-test.jmx

# 命令行模式（无界面）
./bin/jmeter.sh -n -t seckill-test.jmx -l results.jtl -e -o output

# 分布式测试
./bin/jmeter.sh -n -t seckill-test.jmx \
  -R server1,server2,server3 \
  -l results.jtl
```

## 5. 结果分析

### 5.1 关键指标

| 指标 | 说明 | 参考值 |
|------|------|--------|
| TPS | 每秒事务数 | 越高越好 |
| 响应时间 | 平均响应延迟 | <500ms |
| 错误率 | 请求失败比例 | <1% |
| CPU使用率 | 服务器CPU占用 | <80% |

### 5.2 查看HTML报告

```bash
./bin/jmeter -g results.jtl -o html-report/
```

### 5.3 报告目录结构

```
html-report/
├── index.html           # 汇总页面
├── apdex_score.html     # Apdex评分
├── transactions.html    # 事务统计
├── responses_times.html # 响应时间
└── hits.html           # 请求统计
```

## 6. 测试场景

### 6.1 基准测试

- 10个并发用户
- 持续5分钟
- 测试系统基础性能

### 6.2 负载测试

- 100-500并发用户
- 持续10分钟
- 找出性能瓶颈

### 6.3 压力测试

- 500-2000并发用户
- 逐步增加负载
- 测试系统极限

### 6.4 稳定性测试

- 100并发用户
- 持续1小时
- 验证长时间运行稳定性

## 7. 注意事项

1. 测试前确保数据库已初始化
2. 测试环境与生产环境配置一致
3. 监控服务器资源使用
4. 预留足够测试时间
5. 多次测试取平均值
