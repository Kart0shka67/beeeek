# API «Мои задачи» — полная спецификация для фронтенда

**База**: PostgreSQL (`tasksdb`), таблицы `users` и `tasks` создаются автоматически.  
**Порт**: 8081 (в `application.properties`).  
**Авторизация**: Basic Auth во всех `/api/tasks/*` (кроме `/api/users/register` и `/api/users/login`).  
**CORS**: разрешён `*`, методы `GET,POST,PATCH,DELETE,OPTIONS`, заголовки `Content-Type, Authorization`.  
**Swagger UI**: `http://localhost:8081/swagger-ui/index.html` (кнопка Authorize → Basic Auth).  
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

> После успешного логина фронтенд должен сохранять `username:password` и отправлять их в заголовке `Authorization: Basic base64(username:password)` ко всем остальным запросам.

---

## 2. Задачи

### Объект задачи (Task)
```json
{
  "id": 1,
  "title": "Подготовить отчёт",
  "text": "К пятнице сдать",
  "completed": false,
  "userId": 1
}
```
| Поле | Тип | Описание |
|------|-----|----------|
| `id` | long | Уникальный идентификатор (назначает сервер) |
| `title` | string | Название, обязательное, обрезается по краям |
| `text` | string / null | Описание, **необязательное** (может быть null или отсутствовать) |
| `completed` | boolean | Статус выполнения (при создании всегда `false`) |
| `userId` | long | ID владельца (проставляется сервером по авторизованному пользователю) |

---

### 2.1 Получить список задач
**GET** `/api/tasks`  
Требует Basic Auth.

**Query parameters** (опционально)
| Параметр | Тип | Описание |
|----------|-----|----------|
| `userId` | long | Если задан — вернуть только задачи этого пользователя. Если нет — вернуть **все** задачи. |

**Headers**  
`Authorization: Basic base64(username:password)`

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `[{...}, {...}]` | Массив задач (может быть пустым `[]`) |
| `401 Unauthorized` | — | Нет/неверный Basic Auth |

**Примеры**
```
GET /api/tasks                    → все задачи (alice видит и свои, и боба)
GET /api/tasks?userId=1           → только задачи userId=1
GET /api/tasks?userId=999         → [] (пользователя нет, но ошибки нет)
```

---

### 2.2 Получить одну задачу
**GET** `/api/tasks/{id}`  
Требует Basic Auth.

**Path variables**  
| Имя | Тип | Описание |
|-----|-----|----------|
| `id` | long | ID задачи |

**Headers**  
`Authorization: Basic base64(username:password)`

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `{...}` | Задача найдена (любая, даже чужая) |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи с таким ID нет |
| `401 Unauthorized` | — | Нет/неверный Basic Auth |

---

### 2.3 Создать задачу
**POST** `/api/tasks`  
Требует Basic Auth. Владелец = авторизованный пользователь.

**Headers**  
`Content-Type: application/json`  
`Authorization: Basic base64(username:password)`

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
| `401 Unauthorized` | — | Нет/неверный Basic Auth |

**Пример ответа 201**
```json
{
  "id": 5,
  "title": "Подготовить отчёт",
  "text": "К пятнице сдать",
  "completed": false,
  "userId": 1
}
```

---

### 2.4 Завершить задачу
**PATCH** `/api/tasks/{id}/complete`  
Требует Basic Auth. **Только владелец** может завершить.

**Path variables**  
`id` — ID задачи.

**Headers**  
`Authorization: Basic base64(username:password)`

**Request body** — не нужен.

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `200 OK` | `{...}` с `"completed":true` | Успех (повторный вызов тоже `200`, задача остаётся `completed:true`) |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи нет **ИЛИ** это не твоя задача |
| `401 Unauthorized` | — | Нет/неверный Basic Auth |

> Чужая задача для `complete`/`delete` отвечает `404` («как будто её нет»), не `403`.

---

### 2.5 Удалить задачу
**DELETE** `/api/tasks/{id}`  
Требует Basic Auth. **Только владелец** может удалить.

**Path variables**  
`id` — ID задачи.

**Headers**  
`Authorization: Basic base64(username:password)`

**Request body** — не нужен.

**Responses**

| Код | Тело | Когда |
|-----|------|-------|
| `204 No Content` | *(пусто)* | Успешное удаление |
| `404 Not Found` | `{"error":"Task not found"}` | Задачи нет **ИЛИ** это не твоя задача |
| `401 Unauthorized` | — | Нет/неверный Basic Auth |

> После `204` повторный `DELETE` или `GET` по тому же ID вернёт `404`.

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
| Задача не найдена / не твоя | 404 | `Task not found` |
| Username занят | 400 | `Username is taken` |
| Пустой username/password при регистрации | 400 | `Username must not be blank` / `Password must not be blank` |

---

## 4. Примеры для фронтенда (fetch)

```js
// Базовый URL
const BASE = 'http://localhost:8081';
const auth = (user, pass) => 'Basic ' + btoa(`${user}:${pass}`);

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

// 3. Заголовок авторизации для всех дальнейших запросов
const headers = {
  'Content-Type': 'application/json',
  'Authorization': auth('alice', 'alice123')
};

// 4. Создать задачу
const created = await fetch(`${BASE}/api/tasks`, {
  method: 'POST',
  headers,
  body: JSON.stringify({ title: 'Отчёт', text: 'Срочно' })
});
const task = await created.json(); // { id, title, text, completed: false, userId }

// 5. Список всех
const all = await fetch(`${BASE}/api/tasks`, { headers });
const tasks = await all.json();

// 6. Список только alice (userId=1)
const mine = await fetch(`${BASE}/api/tasks?userId=${task.userId}`, { headers });

// 7. Завершить
await fetch(`${BASE}/api/tasks/${task.id}/complete`, {
  method: 'PATCH', headers
});

// 8. Удалить
await fetch(`${BASE}/api/tasks/${task.id}`, {
  method: 'DELETE', headers
});
```

---

## 5. Типичные сценарии интеграции

| Сценарий | Последовательность вызовов |
|----------|----------------------------|
| Пользователь заходит первый раз | `POST /register` → `POST /login` → сохранить креды → `GET /api/tasks` |
| Пользователь возвращается | Взять сохранённые креды → `GET /api/tasks` |
| Создать задачу | `POST /api/tasks` → показать в списке (обновить `GET`) |
| Отметить выполненной | `PATCH /api/tasks/{id}/complete` → обновить UI |
| Удалить | `DELETE /api/tasks/{id}` → убрать из списка |
| Посмотреть чужие задачи | `GET /api/tasks` (видно всё) или `GET /api/tasks?userId=X` |
| Попытаться удалить чужую | `DELETE` → `404` → показать «задача не найдена» |

---

## 6. Запуск бэкенда
```cmd
mvnw.cmd spring-boot:run
```
Приложение поднимется на `http://localhost:8081`.  
Если порт занят — поменяй `server.port` в `src/main/resources/application.properties`.

---

## 7. Полезные ссылки
- Swagger UI: `http://localhost:8081/swagger-ui/index.html` (кнопка **Authorize** → Basic Auth)
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- pgAdmin 4: подключись к `localhost:5432`, база `tasksdb`, таблицы `users` и `tasks`