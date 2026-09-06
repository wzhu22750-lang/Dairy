# xdx-lab Design Style Skill

面向 AI Agent 的全球设计风格 Skill：50 种精选视觉语言、20 位设计大师方法论、120+ 风格知识库，以及一套能校验规范一致性的工具链。这个仓库就是可以直接安装执行的 Skill 本体——只是不包含展示站点和案例截图这类跟"执行"无关的重资产，方便你的 Agent 快速拉取。

## 包含什么

- `SKILL.md`：主执行说明，Agent 的行动手册。
- `references/style-index.md`：50 个精选风格索引。
- `references/styles/`：50 份可执行 `DESIGN.md` 风格规范。
- `references/masters/`：20 位设计大师方法论 `DESIGN.md`。
- `references/全球设计风格知识库.md`：120+ 风格知识库，用于动态生成未收录风格。
- `tools/`：lint、check-output、export、transform 等本地校验和 token 导出工具。

## 想先看效果？

50 种风格 + 20 位大师方法论的完整画廊，以及真实制作的海报、网页、PPT、App UI 案例，都放在展示仓库里：

- 在线画廊：https://xdx888999.github.io/xdx-lab-design-skill/
- 真实案例作品：https://xdx888999.github.io/xdx-lab-design-skill/cases/index.html?v=20260626-real-examples
- 展示仓库源码：https://github.com/xdx888999/xdx-lab-design-skill

## 如何安装

如果你的 Agent 支持通过 GitHub 仓库安装 Skill：

```text
请根据这个 GitHub 仓库帮我安装并启用 design-style Skill：
https://github.com/xdx888999/xdx-lab-design-style-skill

安装后，请读取 SKILL.md。之后当我要求制作网页、PPT、海报、App UI、报告或组件时，请先用它选择风格，再按对应输出路线生成文件。
```

也可以直接克隆：

```bash
git clone https://github.com/xdx888999/xdx-lab-design-style-skill.git
```

## 使用方式

安装或引用后，可以这样触发：

```text
请把这个仓库当作 design-style Skill 使用。
优先读取 SKILL.md；需要选择风格时读取 references/style-index.md；
需要具体规范时读取 references/styles/ 中对应的 DESIGN.md。
如果我指定某位设计大师或“更有大师方法论”，请读取 references/masters/master-index.md，
再读取 references/masters/ 中对应的 DESIGN.md。
```

## 配图规则

如果项目需要配图但用户没有提供素材，本 Skill 默认要求 Agent 使用免版权 / 开放授权 / 可商用免费图库图片，例如 Unsplash、Pexels、Wikimedia Commons 或当前环境可访问的同类来源。无法确认授权时，必须改用生成式图片、SVG 图示、Canvas 图示或 CSS 图案纹理，并说明来源与处理方式。

## 本地验证

```bash
node --test tools/tests/*.test.mjs
node tools/lint.mjs
```

## License

MIT
