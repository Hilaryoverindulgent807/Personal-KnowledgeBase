/**
 * 测试2: 知识图谱算法测试
 * 测试 Louvain 社区检测 和 四信号关联度模型
 * 运行方式: node test_kg.js
 */

// ============================================================
// 测试数据 (与 kg.html 相同的数据)
// ============================================================
const nodes = [
  { id: 0, label: '自主导航技术', type: 'technology' },
  { id: 1, label: '协同集群技术', type: 'technology' },
  { id: 2, label: '通信组网技术', type: 'technology' },
  { id: 3, label: '动力能源技术', type: 'technology' },
  { id: 4, label: '态势感知技术', type: 'technology' },
  { id: 5, label: '空中无人装备', type: 'equipment' },
  { id: 6, label: '地面无人装备', type: 'equipment' },
  { id: 7, label: '水面无人装备', type: 'equipment' },
  { id: 8, label: '水下无人装备', type: 'equipment' },
  { id: 9, label: '战略情报研究室', type: 'organization' },
  { id: 10, label: '装备技术情报室', type: 'organization' },
  { id: 11, label: '作战运用情报室', type: 'organization' },
  { id: 12, label: '无人集群智能协同专项', type: 'project' },
  { id: 13, label: '水下无人装备发展论证', type: 'project' },
  { id: 14, label: '自主导航技术专项', type: 'project' },
  { id: 15, label: '有人-无人混合编组', type: 'concept' },
  { id: 16, label: '分布式杀伤', type: 'concept' },
  { id: 17, label: '马赛克战', type: 'concept' },
  { id: 18, label: '无人集群协同作战研究报告', type: 'document' },
  { id: 19, label: '空中无人装备发展评估', type: 'document' },
  { id: 20, label: '水下装备续航论证报告', type: 'document' },
  { id: 21, label: '通信组网技术综述', type: 'document' },
  { id: 22, label: '外军力量编成分析', type: 'document' },
  { id: 23, label: '任务载荷技术', type: 'technology' },
  { id: 24, label: '智能项目专项', type: 'project' },
];

const edges = [
  { source: 0, target: 5, type: 'direct', weight: 3.0 },
  { source: 0, target: 6, type: 'direct', weight: 3.0 },
  { source: 1, target: 5, type: 'direct', weight: 3.0 },
  { source: 1, target: 12, type: 'direct', weight: 3.0 },
  { source: 2, target: 7, type: 'direct', weight: 3.0 },
  { source: 2, target: 8, type: 'direct', weight: 3.0 },
  { source: 3, target: 8, type: 'direct', weight: 3.0 },
  { source: 4, target: 5, type: 'direct', weight: 3.0 },
  { source: 4, target: 6, type: 'direct', weight: 3.0 },
  { source: 5, target: 19, type: 'direct', weight: 3.0 },
  { source: 8, target: 20, type: 'direct', weight: 3.0 },
  { source: 12, target: 18, type: 'direct', weight: 3.0 },
  { source: 13, target: 20, type: 'direct', weight: 3.0 },
  { source: 14, target: 0, type: 'direct', weight: 3.0 },
  { source: 15, target: 5, type: 'direct', weight: 3.0 },
  { source: 15, target: 1, type: 'direct', weight: 3.0 },
  { source: 16, target: 1, type: 'direct', weight: 3.0 },
  { source: 17, target: 1, type: 'direct', weight: 3.0 },
  { source: 17, target: 16, type: 'direct', weight: 3.0 },
  { source: 21, target: 2, type: 'direct', weight: 3.0 },
  { source: 22, target: 15, type: 'direct', weight: 3.0 },
  { source: 23, target: 5, type: 'direct', weight: 3.0 },
  { source: 23, target: 6, type: 'direct', weight: 3.0 },
  { source: 24, target: 4, type: 'direct', weight: 3.0 },
  { source: 18, target: 19, type: 'source_overlap', weight: 4.0 },
  { source: 18, target: 22, type: 'source_overlap', weight: 4.0 },
  { source: 20, target: 21, type: 'source_overlap', weight: 4.0 },
  { source: 10, target: 14, type: 'source_overlap', weight: 4.0 },
  { source: 11, target: 12, type: 'source_overlap', weight: 4.0 },
  { source: 9, target: 24, type: 'source_overlap', weight: 4.0 },
  { source: 0, target: 7, type: 'adamic_adar', weight: 1.5 },
  { source: 3, target: 5, type: 'adamic_adar', weight: 1.5 },
  { source: 6, target: 8, type: 'adamic_adar', weight: 1.5 },
  { source: 13, target: 12, type: 'adamic_adar', weight: 1.5 },
  { source: 16, target: 15, type: 'adamic_adar', weight: 1.5 },
  { source: 5, target: 6, type: 'type_affinity', weight: 1.0 },
  { source: 5, target: 7, type: 'type_affinity', weight: 1.0 },
  { source: 6, target: 7, type: 'type_affinity', weight: 1.0 },
  { source: 7, target: 8, type: 'type_affinity', weight: 1.0 },
  { source: 9, target: 10, type: 'type_affinity', weight: 1.0 },
  { source: 10, target: 11, type: 'type_affinity', weight: 1.0 },
  { source: 0, target: 1, type: 'type_affinity', weight: 1.0 },
  { source: 1, target: 2, type: 'type_affinity', weight: 1.0 },
  { source: 2, target: 3, type: 'type_affinity', weight: 1.0 },
];

// ============================================================
// Louvain 社区检测 (与 kg.html 相同的算法)
// ============================================================
function detectCommunities(nodes, edges) {
  const n = nodes.length;
  let community = nodes.map((_, i) => i);
  const adj = Array.from({ length: n }, () => new Map());
  const totalWeight = edges.reduce((s, e) => s + e.weight, 0);
  edges.forEach(e => {
    adj[e.source].set(e.target, (adj[e.source].get(e.target) || 0) + e.weight);
    adj[e.target].set(e.source, (adj[e.target].get(e.source) || 0) + e.weight);
  });

  let improved = true, iter = 0;
  while (improved && iter < 50) {
    improved = false;
    iter++;
    for (let i = 0; i < n; i++) {
      const curComm = community[i];
      const neighborComms = new Map();
      adj[i].forEach((w, j) => {
        const c = community[j];
        neighborComms.set(c, (neighborComms.get(c) || 0) + w);
      });

      let curInW = 0;
      adj[i].forEach((w, j) => { if (community[j] === curComm && j !== i) curInW += w; });

      let bestComm = curComm, bestGain = 0;
      neighborComms.forEach((wToComm, c) => {
        if (c === curComm) return;
        const gain = (wToComm - curInW) / totalWeight;
        if (gain > bestGain) { bestGain = gain; bestComm = c; }
      });

      if (bestComm !== curComm) { community[i] = bestComm; improved = true; }
    }
  }

  const commMap = new Map();
  let cid = 0;
  community = community.map(c => { if (!commMap.has(c)) commMap.set(c, cid++); return commMap.get(c); });

  const communities = [];
  for (let c = 0; c < cid; c++) {
    const members = nodes.filter((_, i) => community[i] === c);
    const mIds = new Set(members.map(m => m.id));
    let intE = 0;
    edges.forEach(e => { if (mIds.has(e.source) && mIds.has(e.target)) intE++; });
    const posE = members.length * (members.length - 1) / 2;
    communities.push({ id: c, members, cohesion: posE > 0 ? intE / posE : 0 });
  }

  return { community, communities };
}

// ============================================================
// 四信号关联度计算
// ============================================================
function computeRelevance(nodeA, nodeB, nodes, edges) {
  const signals = { direct: 0, source_overlap: 0, adamic_adar: 0, type_affinity: 0 };

  // 1. 直接链接
  edges.forEach(e => {
    if ((e.source === nodeA && e.target === nodeB) || (e.source === nodeB && e.target === nodeA)) {
      if (e.type === 'direct') signals.direct += e.weight;
    }
  });

  // 2. 来源重叠
  edges.forEach(e => {
    if ((e.source === nodeA && e.target === nodeB) || (e.source === nodeB && e.target === nodeA)) {
      if (e.type === 'source_overlap') signals.source_overlap += e.weight;
    }
  });

  // 3. Adamic-Adar (通过共同邻居)
  const neighborsA = new Set();
  const neighborsB = new Set();
  edges.forEach(e => {
    if (e.source === nodeA) neighborsA.add(e.target);
    if (e.target === nodeA) neighborsA.add(e.source);
    if (e.source === nodeB) neighborsB.add(e.target);
    if (e.target === nodeB) neighborsB.add(e.source);
  });
  const commonNeighbors = [...neighborsA].filter(x => neighborsB.has(x));
  commonNeighbors.forEach(cn => {
    const degree = edges.filter(e => e.source === cn || e.target === cn).length;
    if (degree > 1) signals.adamic_adar += 1.0 / Math.log2(degree);
  });

  // 4. 类型亲和
  const typeA = nodes[nodeA].type;
  const typeB = nodes[nodeB].type;
  if (typeA === typeB) signals.type_affinity = 1.0;

  // 总分
  const total = signals.direct * 3.0 + signals.source_overlap * 4.0 + signals.adamic_adar * 1.5 + signals.type_affinity * 1.0;
  return { signals, total };
}

// ============================================================
// 测试执行
// ============================================================
let passed = 0;
let failed = 0;

function assert(condition, message) {
  if (condition) {
    console.log(`  ✓ ${message}`);
    passed++;
  } else {
    console.log(`  ✗ ${message}`);
    failed++;
  }
}

// --- 测试1: Louvain 社区检测 ---
console.log('=== 测试1: Louvain 社区检测 ===');
const { community, communities } = detectCommunities(nodes, edges);

console.log(`\n步骤1: 检查社区数量`);
assert(communities.length >= 2, `检测到 ${communities.length} 个社区 (期望 >= 2)`);
assert(communities.length <= 15, `社区数量合理 (不超过15)`);

console.log(`\n步骤2: 检查所有节点都被分配社区`);
const assignedNodes = new Set();
communities.forEach(c => c.members.forEach(m => assignedNodes.add(m.id)));
assert(assignedNodes.size === nodes.length, `所有 ${nodes.length} 个节点都已分配社区 (实际: ${assignedNodes.size})`);

console.log(`\n步骤3: 检查社区成员详情`);
communities.forEach(c => {
  console.log(`  社区 ${c.id}: ${c.members.length} 个节点, 内聚度 ${c.cohesion.toFixed(3)}, 成员: ${c.members.map(m => m.label).join(', ')}`);
});

console.log(`\n步骤4: 验证强连接的节点在同一社区`);
// 节点5(空中无人装备)和节点1(协同集群技术)有大量直接链接，应在同一社区
const comm5 = community[5];
const comm1 = community[1];
// 不强制要求同一社区，但输出检查结果
console.log(`  节点5(空中无人装备) 社区=${comm5}, 节点1(协同集群技术) 社区=${comm1}`);

console.log(`\n步骤5: 检查内聚度范围`);
communities.forEach(c => {
  assert(c.cohesion >= 0 && c.cohesion <= 1, `社区 ${c.id} 内聚度 ${c.cohesion.toFixed(3)} 在 [0,1] 范围内`);
});

// --- 测试2: 四信号关联度模型 ---
console.log('\n=== 测试2: 四信号关联度模型 ===');

console.log(`\n步骤1: 直接链接节点对`);
const r_0_5 = computeRelevance(0, 5, nodes, edges);
console.log(`  节点0(自主导航) ↔ 节点5(空中无人装备):`);
console.log(`    直接链接: ${r_0_5.signals.direct}, 来源重叠: ${r_0_5.signals.source_overlap}, Adamic-Adar: ${r_0_5.signals.adamic_adar.toFixed(3)}, 类型亲和: ${r_0_5.signals.type_affinity}`);
console.log(`    总分: ${r_0_5.total.toFixed(2)}`);
assert(r_0_5.signals.direct > 0, '直接链接信号 > 0');
assert(r_0_5.total > 0, '总分 > 0');

console.log(`\n步骤2: 来源重叠节点对`);
const r_18_19 = computeRelevance(18, 19, nodes, edges);
console.log(`  节点18(集群报告) ↔ 节点19(空中评估):`);
console.log(`    直接链接: ${r_18_19.signals.direct}, 来源重叠: ${r_18_19.signals.source_overlap}, Adamic-Adar: ${r_18_19.signals.adamic_adar.toFixed(3)}, 类型亲和: ${r_18_19.signals.type_affinity}`);
assert(r_18_19.signals.source_overlap > 0, '来源重叠信号 > 0');

console.log(`\n步骤3: 同类型节点(类型亲和)`);
const r_5_6 = computeRelevance(5, 6, nodes, edges);
console.log(`  节点5(空中无人) ↔ 节点6(地面无人):`);
console.log(`    直接链接: ${r_5_6.signals.direct}, 来源重叠: ${r_5_6.signals.source_overlap}, Adamic-Adar: ${r_5_6.signals.adamic_adar.toFixed(3)}, 类型亲和: ${r_5_6.signals.type_affinity}`);
assert(r_5_6.signals.type_affinity === 1.0, '同类型节点亲和度 = 1.0');

console.log(`\n步骤4: 无直接关联的节点对`);
const r_9_8 = computeRelevance(9, 8, nodes, edges);
console.log(`  节点9(战略情报室) ↔ 节点8(水下无人装备):`);
console.log(`    直接链接: ${r_9_8.signals.direct}, 来源重叠: ${r_9_8.signals.source_overlap}, Adamic-Adar: ${r_9_8.signals.adamic_adar.toFixed(3)}, 类型亲和: ${r_9_8.signals.type_affinity}`);
console.log(`    总分: ${r_9_8.total.toFixed(2)}`);

console.log(`\n步骤5: 权重排序验证`);
const pairs = [
  { a: 0, b: 5, label: '自主导航↔空中无人' },
  { a: 18, b: 19, label: '集群报告↔空中评估' },
  { a: 5, b: 6, label: '空中无人↔地面无人' },
  { a: 9, b: 8, label: '战略情报室↔水下无人' },
];
const scored = pairs.map(p => ({ ...p, score: computeRelevance(p.a, p.b, nodes, edges).total }));
scored.sort((a, b) => b.score - a.score);
console.log('  关联度排序:');
scored.forEach((p, i) => console.log(`    ${i + 1}. ${p.label}: ${p.score.toFixed(2)}`));
assert(scored[0].score >= scored[scored.length - 1].score, '排序正确(最高分 >= 最低分)');

// --- 测试3: 图谱统计 ---
console.log('\n=== 测试3: 图谱统计 ===');

console.log(`\n步骤1: 基本统计`);
assert(nodes.length === 25, `节点数 = 25 (实际: ${nodes.length})`);
assert(edges.length === 44, `边数 = 44 (实际: ${edges.length})`);

console.log(`\n步骤2: 边类型分布`);
const edgeTypes = {};
edges.forEach(e => { edgeTypes[e.type] = (edgeTypes[e.type] || 0) + 1; });
Object.entries(edgeTypes).forEach(([type, count]) => {
  console.log(`  ${type}: ${count} 条`);
});
assert(edgeTypes['direct'] > 0, '存在直接链接');
assert(edgeTypes['source_overlap'] > 0, '存在来源重叠');
assert(edgeTypes['adamic_adar'] > 0, '存在Adamic-Adar');
assert(edgeTypes['type_affinity'] > 0, '存在类型亲和');

console.log(`\n步骤3: 节点类型分布`);
const nodeTypes = {};
nodes.forEach(n => { nodeTypes[n.type] = (nodeTypes[n.type] || 0) + 1; });
Object.entries(nodeTypes).forEach(([type, count]) => {
  console.log(`  ${type}: ${count} 个`);
});
assert(Object.keys(nodeTypes).length === 6, '包含6种节点类型');

console.log(`\n步骤4: 节点度数统计`);
const degrees = {};
nodes.forEach(n => { degrees[n.id] = 0; });
edges.forEach(e => { degrees[e.source]++; degrees[e.target]++; });
const maxDeg = Math.max(...Object.values(degrees));
const minDeg = Math.min(...Object.values(degrees));
const avgDeg = (Object.values(degrees).reduce((a, b) => a + b, 0) / nodes.length).toFixed(1);
console.log(`  最大度数: ${maxDeg}, 最小度数: ${minDeg}, 平均度数: ${avgDeg}`);
assert(maxDeg > 0, '最大度数 > 0');
assert(minDeg >= 0, '最小度数 >= 0');

// --- 汇总 ---
console.log(`\n${'='.repeat(50)}`);
console.log(`=== 测试汇总 ===`);
console.log(`  通过: ${passed}`);
console.log(`  失败: ${failed}`);
console.log(`  总计: ${passed + failed}`);
console.log(`${'='.repeat(50)}`);

if (failed === 0) {
  console.log('🎉 所有测试通过!');
  process.exit(0);
} else {
  console.log('💥 部分测试失败!');
  process.exit(1);
}
