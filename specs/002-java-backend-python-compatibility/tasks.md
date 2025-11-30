# Tasks: Java后端Python版本兼容完善

**Input**: Design documents from `/specs/002-java-backend-python-compatibility/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: The feature spec requires comprehensive compatibility testing between Java and Python backends.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Web app**: `backend/src/main/java/com/redink/`, `frontend/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 [P] Create Spring Boot project structure per implementation plan in backend/
- [ ] T002 [P] Configure pom.xml with Spring Boot 3.x and Spring AI 1.1.0 dependencies
- [ ] T003 [P] Create application.yml configuration file in backend/src/main/resources/
- [ ] T004 Create RedInkApplication.java main class in backend/src/main/java/com/redink/
- [ ] T005 [P] Setup package structure: config/, controller/, exception/, model/, service/, util/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T006 Create GlobalExceptionHandler.java in backend/src/main/java/com/redink/exception/
- [ ] T007 [P] Create base request/response models in backend/src/main/java/com/redink/model/
- [ ] T008 [P] Setup CORS configuration in backend/src/main/java/com/redink/config/
- [ ] T009 Create OpenAIConfig.java for AI model configuration in backend/src/main/java/com/redink/config/
- [ ] T010 Create JsonUtil.java for JSON utilities in backend/src/main/java/com/redink/util/

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - API接口功能完整兼容 (Priority: P1) 🎯 MVP

**Goal**: 实现所有核心API接口，确保与Python后端完全兼容，包括大纲生成和图片生成功能

**Independent Test**: 通过Postman或其他API测试工具，分别向Python和Java后端发送相同的请求，比较响应结果是否一致

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T011 [P] [US1] Contract test for /api/outline endpoint in backend/tests/contract/test_outline.py
- [ ] T012 [P] [US1] Contract test for /api/generate endpoint in backend/tests/contract/test_generate.py
- [ ] T013 [P] [US1] Integration test for outline generation flow in backend/tests/integration/test_outline_flow.py
- [ ] T014 [P] [US1] Integration test for image generation flow in backend/tests/integration/test_generate_flow.py

### Implementation for User Story 1

- [ ] T015 [P] [US1] Create outline request/response models in backend/src/main/java/com/redink/model/request/OutlineRequest.java
- [ ] T016 [P] [US1] Create generate request/response models in backend/src/main/java/com/redink/model/request/GenerateRequest.java
- [ ] T017 [P] [US1] Create task response models in backend/src/main/java/com/redink/model/response/TaskResponse.java
- [ ] T018 [US1] Implement OutlineService.java with AI model integration in backend/src/main/java/com/redink/service/OutlineService.java
- [ ] T019 [US1] Implement ImageService.java with AI model integration in backend/src/main/java/com/redink/service/ImageService.java
- [ ] T020 [US1] Create ApiController.java with outline and generate endpoints in backend/src/main/java/com/redink/controller/ApiController.java
- [ ] T021 [US1] Add validation and error handling for US1 endpoints
- [ ] T022 [US1] Add logging for outline and image generation operations

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - API接口行为一致 (Priority: P2)

**Goal**: 完善所有API接口的响应格式，确保错误处理和状态码与Python后端完全一致

**Independent Test**: 模拟各种请求情况（正常请求、缺少参数、无效格式、资源不存在），验证Java后端的响应是否与Python后端一致

### Tests for User Story 2

- [ ] T023 [P] [US2] Contract test for /api/retry endpoint in backend/tests/contract/test_retry.py
- [ ] T024 [P] [US2] Contract test for /api/status endpoint in backend/tests/contract/test_status.py
- [ ] T025 [P] [US2] Contract test for /api/download endpoint in backend/tests/contract/test_download.py
- [ ] T026 [P] [US2] Integration test for error handling scenarios in backend/tests/integration/test_error_handling.py
- [ ] T027 [P] [US2] Integration test for retry mechanisms in backend/tests/integration/test_retry_mechanisms.py

### Implementation for User Story 2

- [ ] T028 [P] [US2] Create retry request/response models in backend/src/main/java/com/redink/model/request/RetryRequest.java
- [ ] T029 [P] [US2] Create status response models in backend/src/main/java/com/redink/model/response/StatusResponse.java
- [ ] T030 [P] [US2] Create download response models in backend/src/main/java/com/redink/model/response/DownloadResponse.java
- [ ] T031 [US2] Implement HistoryService.java for history management in backend/src/main/java/com/redink/service/HistoryService.java
- [ ] T032 [US2] Add retry endpoints to ApiController.java (single and batch retry)
- [ ] T033 [US2] Add task status query endpoint to ApiController.java
- [ ] T034 [US2] Add image download endpoint to ApiController.java
- [ ] T035 [US2] Create HistoryController.java with all history endpoints in backend/src/main/java/com/redink/controller/HistoryController.java
- [ ] T036 [US2] Implement comprehensive error handling matching Python backend responses
- [ ] T037 [US2] Add health check endpoint to ApiController.java

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - 历史记录管理 (Priority: P3)

**Goal**: 完善历史记录管理接口，确保与Python后端的历史记录功能完全兼容

**Independent Test**: 测试历史记录的创建、查询、更新、删除和搜索功能，验证与Python后端的一致性

### Tests for User Story 3

- [ ] T038 [P] [US3] Contract test for /api/history/list endpoint in backend/tests/contract/test_history_list.py
- [ ] T039 [P] [US3] Contract test for /api/history/{id} endpoint in backend/tests/contract/test_history_detail.py
- [ ] T040 [P] [US3] Contract test for /api/history/search endpoint in backend/tests/contract/test_history_search.py
- [ ] T041 [P] [US3] Integration test for history CRUD operations in backend/tests/integration/test_history_crud.py
- [ ] T042 [P] [US3] Integration test for history download functionality in backend/tests/integration/test_history_download.py

### Implementation for User Story 3

- [ ] T043 [P] [US3] Create history entity models in backend/src/main/java/com/redink/model/entity/HistoryEntity.java
- [ ] T044 [P] [US3] Create history request/response models in backend/src/main/java/com/redink/model/request/HistoryRequest.java
- [ ] T045 [P] [US3] Create history list/response models in backend/src/main/java/com/redink/model/response/HistoryListResponse.java
- [ ] T046 [US3] Implement HistoryService.java CRUD operations (depends on T043)
- [ ] T047 [US3] Add history list endpoint to HistoryController.java
- [ ] T048 [US3] Add history detail endpoint to HistoryController.java
- [ ] T049 [US3] Add history update endpoint to HistoryController.java
- [ ] T050 [US3] Add history delete endpoint to HistoryController.java
- [ ] T051 [US3] Add history search endpoint to HistoryController.java
- [ ] T052 [US3] Add history statistics endpoint to HistoryController.java
- [ ] T053 [US3] Add task scanning endpoints to HistoryController.java
- [ ] T054 [US3] Add history download endpoint to HistoryController.java

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T055 [P] Comprehensive compatibility testing between Java and Python backends
- [ ] T056 Code cleanup and refactoring across all controllers and services
- [ ] T057 Performance optimization for concurrent request handling
- [ ] T058 [P] Add unit tests for all service classes in backend/tests/unit/
- [ ] T059 Security hardening: input validation, sanitization, and rate limiting
- [ ] T060 Run quickstart.md validation and update documentation
- [ ] T061 [P] Update OpenAPI specification with all implemented endpoints
- [ ] T062 Final integration test with frontend application

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Builds upon US1 but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent feature addition

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before controllers/endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Contract test for /api/outline endpoint in backend/tests/contract/test_outline.py"
Task: "Contract test for /api/generate endpoint in backend/tests/contract/test_generate.py"
Task: "Integration test for outline generation flow in backend/tests/integration/test_outline_flow.py"
Task: "Integration test for image generation flow in backend/tests/integration/test_generate_flow.py"

# Launch all models for User Story 1 together:
Task: "Create outline request/response models in backend/src/main/java/com/redink/model/request/OutlineRequest.java"
Task: "Create generate request/response models in backend/src/main/java/com/redink/model/request/GenerateRequest.java"
Task: "Create task response models in backend/src/main/java/com/redink/model/response/TaskResponse.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

## Task Summary

**Total Tasks**: 62
- **Setup Phase**: 5 tasks
- **Foundational Phase**: 5 tasks
- **User Story 1 (P1 - MVP)**: 12 tasks
- **User Story 2 (P2)**: 15 tasks
- **User Story 3 (P3)**: 12 tasks
- **Polish Phase**: 8 tasks

**Parallel Opportunities**: 32 tasks marked with [P]

**Independent Test Criteria**:
- **US1**: API responses match Python backend format and content
- **US2**: Error handling and status codes match Python backend behavior
- **US3**: History management functionality fully compatible with Python backend
