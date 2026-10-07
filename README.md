# API «Мои задачи» — полная спецификация для фронтенда

**База**: PostgreSQL (`tasksdb`), таблицы `users`, `tasks`, `task_assignees` создаются автоматически.  
**Порт**: 8081 (в `application.properties`).  
**Авторизация**: **нет** — все эндпоинты открыты. Регистрация и логин пользователей работают для идентификации владельца задачи.  
**Роли**: `USER` / `ADMIN` (в базе, для будущего расширения).  
**CORS**: разрешён `*`, методы `GET,POST,PATCH,DELETE,OPTIONS`, заголовки `Content-Type`.  
**Swagger UI**: `http://localhost:8081/swagger-ui/index.html` (авторизация не требуется).  
**JSON-схема**: `http://localhost:8081/v3/api-docs`.

---

## 1. Пользователи

### 1.1 Регистрация
**POST** `/api/users/register`  
Публичный, без авторизации.

**Headers**  
`Content-Type: application/json`

**Request body**
```json
{
  "username": "alice",
  "password": "alice123"
}
```
- `username` (string, required) — уникальный, не пустой
- `password` (string, required) — не пустой

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `201 Created` | `{"id":1,"username":"alice"}` | Успех |
| `400 Bad Request` | `{"error":"Username must not be blank"}` | Пустой username |
| `400 Bad Request` | `{"error":"Password must not be blank"}` | Пустой password |
| `400 Bad Request` | `{"error":"Username is taken"}` | Username уже существует |

---

### 1.2 Вход (логин)
**POST** `/api/users/login`  
Публичный, без авторизации. Возвращает данные пользователя для проверки.

**Headers**  
`Content-Type: application/json`

**Request body** — такой же как при регистрации.

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `{"id":1,"username":"alice"}` | Успешный вход |
| `401 Unauthorized` | `{"error":"Invalid username or password"}` | Неверный пароль или пользователь не найден |

---

### 1.3 Список всех пользователей
**GET** `/api/users`  
Публичный, без авторизации. Возвращает массив `{id, username}` для выбора получателей задачи.

**Responses**
| Код | Тело |
|-----|------|
| `200 OK` | `[{"id":1,"username":"alice"},{"id":2,"username":"bob"}]` |

---

## 2. Задачи

### Объект задачи (Task)
```json
{
  "id": 1,
  "title": "Подготовить отчёт",
  "text": "К пятнице сдать",
  "completed": false,
  "deleted": false,
  "ownerId": 1,
  "assignees": [{"id":2,"username":"bob"}]
}
```
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | long | Уникальный идентификатор (назначает сервер) |
| `title` | string | Название, обязательное, обрезается по краям |
| `text` | string / null | Описание, **необязательное** |
| `completed` | boolean | Статус выполнения (при создании всегда `false`) |
| `deleted` | boolean | Soft delete флаг (админ видит, пользователь — нет) |
| `ownerId` | long | ID владельца (кто создал) |
| `assignees` | array | Массив пользователей, которым назначена задача |

---

### 2.1 Получить список задач
**GET** `/api/tasks`  
Публичный, без авторизации. Возвращает все активные задачи.

**Query parameters** (опционально)
| Параметр | Тип | Описание |
|----------|-----|----------|
| `userId` | long | Если задан — вернуть только задачи этого пользователя. Если нет — вернуть **все** задачи. |

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `[{...}, {...}]` | Массив задач (может быть пустым `[]`) |

**Примеры**
```
GET /api/tasks                    → все задачи
GET /api/tasks?userId=1           → только задачи userId=1
GET /api/tasks?userId=999         → [] (пользователя нет, но ошибки нет)
```

---

### 2.2 Получить одну задачу
**GET** `/api/tasks/{id}`  
Публичный, без авторизации.

**Path variables**  
| Имя | Тип | Описание |
|-----|-----|----------|
| `id` | long | ID задачи |

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `{...}` | Задача найдена |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи с таким ID нет |

---

### 2.3 Создать задачу
**POST** `/api/tasks`  
Публичный, без авторизации. Владелец не назначается (ownerId = 0).

**Headers**  
`Content-Type: application/json`

**Request body**
```json
{
  "title": "  Подготовить отчёт  ",
  "text": "К пятнице сдать"
}
```
- `title` (string, **required**) — не пустой после `trim()`, пробелы по краям удаляются
- `text` (string, **optional**) — может быть опущено, `null` или пустой строкой

**Responses**

| Код | Тело / Заголовки | Когда |
|-----|------------------|-------|
| `201 Created` | `Location: /api/tasks/{id}` + тело задачи | Успех |
| `400 Bad Request` | `{"error":"Title must not be blank"}` | title пустой / из пробелов / отсутствует |

**Пример ответа 201**
```json
{
  "id": 5,
  "title": "Подготовить отчёт",
  "text": "К пятнице сдать",
  "completed": false,
  "deleted": false,
  "ownerId": 1,
  "assignees": []
}
```

---

### 2.4 Завершить задачу
**PATCH** `/api/tasks/{id}/complete`  
Публичный, без авторизации.

**Path variables**  
`id` — ID задачи.

**Request body** — не нужен.

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `{...}` с `"completed":true` | Успех (повторный вызов тоже `200`, задача остаётся `completed:true`) |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи нет |

---

### 2.5 Удалить задачу (soft delete)
**DELETE** `/api/tasks/{id}`  
Публичный, без авторизации. Soft delete — задача помечается `deleted=true`.

**Path variables**  
`id` — ID задачи.

**Request body** — не нужен.

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `204 No Content` | *(пусто)* | Успешное мягкое удаление |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи нет |

> После мягкого удаления задача исчезает из списков (`GET /api/tasks`), но видна в `GET /api/tasks/deleted`.

---

### 2.6 Создать задачу и назначить нескольким пользователям
**POST** `/api/tasks/assign`  
Публичный, без авторизации. Владелец не назначается, получатели — по `assigneeIds`.

**Headers**  
`Content-Type: application/json`

**Request body**
```json
{
  "title": "Задача для команды",
  "text": "Сделать к пятнице",
  "assigneeIds": [2, 3]
}
```
- `title` (string, **required**) — не пустой после `trim()`
- `text` (string, **optional**)
- `assigneeIds` (array of long, **optional**) — ID пользователей, которым назначается задача

**Responses**

| Код | Тело / Заголовки | Когда |
|-----|------------------|-------|
| `201 Created` | `Location: /api/tasks/{id}` + тело задачи с `assignees` | Успех |
| `400 Bad Request` | `{"error":"Title must not be blank"}` | title пустой |

---

### 2.7 Раскомплит (вернуть в активные)
**PATCH** `/api/tasks/{id}/uncomplete`  
Публичный, без авторизации.

**Path variables**  
`id` — ID задачи.

**Request body** — не нужен.

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `{...}` с `"completed":false` | Успех |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи нет |

---

### 2.8 Админ: восстановить удалённую задачу
**PATCH** `/api/tasks/{id}/restore`  
Публичный, без авторизации.

**Path variables**  
`id` — ID задачи.

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `{...}` с `"deleted":false` | Успех |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи нет |

---

### 2.9 Админ: все активные задачи
**GET** `/api/tasks/all`  
Публичный, без авторизации. Возвращает все задачи со `deleted=false`.

---

### 2.10 Админ: все удалённые задачи
**GET** `/api/tasks/deleted`  
Публичный, без авторизации. Возвращает задачи со `deleted=true`.

---

### 2.11 Админ: задачи конкретного пользователя
**GET** `/api/tasks/user/{userId}`  
Публичный, без авторизации. Возвращает все задачи пользователя (включая удалённые).

---

## 3. Формат ошибок
Все ошибки возвращают единый JSON:
```json
{ "error": "Текст ошибки" }
```
| Ошибка | HTTP код | Текст |
|--------|----------|-------|
| Пустое/отсутствующее название | 400 | `Title must not be blank` |
| Неверный логин/пароль | 401 | `Invalid username or password` |
| Задача не найдена | 404 | `Task not found` |
| Username занят | 400 | `Username is taken` |
| Пустой username/password при регистрации | 400 | `Username must not be blank` / `Password must not be blank` |

---

## 4. Примеры для фронтенда (fetch)

```js
// Базовый URL
const BASE = 'http://localhost:8081';

// 1. Регистрация
await fetch(`${BASE}/api/users/register`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'alice', password: 'alice123' })
});

// 2. Логин (проверка)
const login = await fetch(`${BASE}/api/users/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'alice', password: 'alice123' })
});
const { id, username } = await login.json();

// 3. Создать задачу
const created = await fetch(`${BASE}/api/tasks`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ title: 'Отчёт', text: 'Срочно' })
});
const task = await created.json(); // { id, title, text, completed: false, deleted: false, ownerId: 0 }

// 4. Список всех
const all = await fetch(`${BASE}/api/tasks`);
const tasks = await all.json();

// 5. Список только alice (userId=1)
const mine = await fetch(`${BASE}/api/tasks?userId=${task.ownerId}`);

// 6. Завершить
await fetch(`${BASE}/api/tasks/${task.id}/complete`, { method: 'PATCH' });

// 7. Раскомплит
await fetch(`${BASE}/api/tasks/${task.id}/uncomplete`, { method: 'PATCH' });

// 8. Удалить (soft delete)
await fetch(`${BASE}/api/tasks/${task.id}`, { method: 'DELETE' });

// 9. Назначить задачу нескольким пользователям
await fetch(`${BASE}/api/tasks/assign`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ title: 'Командная задача', text: 'Сделать вместе', assigneeIds: [2, 3] })
});

// 10. Админ: восстановить удалённую задачу
await fetch(`${BASE}/api/tasks/${task.id}/restore`, { method: 'PATCH' });

// 11. Админ: посмотреть все задачи
const allTasks = await fetch(`${BASE}/api/tasks/all`);
const allTasksData = await allTasks.json();

// 13. Админ: посмотреть удалённые задачи
const deletedTasks = await fetch(`${BASE}/api/tasks/deleted`);
const deletedTasksData = await deletedTasks.json();
```

---

## 5. Типичные сценарии интеграции

| Сценарий | Последовательность вызовов |
|----------|----------------------------|
| Пользователь заходит первый раз | `POST /register` → `POST /login` → `GET /api/tasks` |
| Пользователь возвращается | `GET /api/tasks` |
| Создать задачу | `POST /api/tasks` → показать в списке (обновить `GET`) |
| Отметить выполненной | `PATCH /api/tasks/{id}/complete` → обновить UI |
| Раскомплит | `PATCH /api/tasks/{id}/uncomplete` → обновить UI |
| Удалить (soft delete) | `DELETE /api/tasks/{id}` → убрать из списка |
| Назначить задачу команде | `POST /api/tasks/assign` с `assigneeIds` → получатели видят в `GET /api/tasks` |
| Админ: посмотреть все задачи | `GET /api/tasks/all` |
| Админ: посмотреть удалённые | `GET /api/tasks/deleted` |
| Админ: восстановить задачу | `PATCH /api/tasks/{id}/restore` |
| Админ: задачи пользователя | `GET /api/tasks/user/{userId}` |

---

## 6. Запуск бэкенда
```cmd
mvnw.cmd spring-boot:run
```
Приложение поднимется на `http://localhost:8081`.  
Если порт занят — поменяй `server.port` в `src/main/resources/application.properties`.

---

## 7. Полезные ссылки
- Swagger UI: `http://localhost:8081/swagger-ui/index.html` (авторизация не требуется)
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- pgAdmin 4: подключись к `localhost:5432`, база `tasksdb`, таблицы `users`, `tasks`, `task_assignees`