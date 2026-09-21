# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

本仓库的项目事实（项目概述、常用命令、环境与测试基础设施、代码结构、开发约定、已知坑）统一维护在 `AGENTS.md`，由下方导入自动加载。修改项目事实时**只改 `AGENTS.md`**，不要在本文件重复维护，避免两处漂移。

@AGENTS.md

## 补充说明（AGENTS.md 未覆盖的部分）

- **本地开发配置**：`src/main/resources/application-dev.yml`（dev profile）已被 `.gitignore` 忽略，含真实凭据。新环境请以 `src/main/resources/application-example.yaml` 为模板自行创建；端口等以本地该文件为准，勿把其中的凭据写入任何被跟踪的文件。
- **设计文档**：`document/design/`（`user.md` / `music.md` / `album.md`）描述各模块设计。做用户、音乐、专辑相关改动前先读对应文档；数据库 schema 以 `document/database/heimusic.md` 为唯一基准（变更流程见 AGENTS.md 已知坑一节）。
- **部署**：`document/deploy/README.md` + `document/deploy/docker-compose.yaml`（全套基础设施），应用配置示例在 `document/deploy/conf/application-example.yaml`。
