
## Витрина интернет-магазина (Spring Boot, Multi-Module)

Проект представляет собой веб-приложение «Витрина интернет-магазина» с функционалом просмотра товаров, управления 
корзиной и оформлением заказов, состоящий из двух модулей: 
* market-service 
* payment-service.

###  Стек технологий
#### Общий стек
* Java 21
* Spring Boot 3.5.7
* Spring Security с Keycloak авторизацией
* Gradle (multi-module)
* Docker / Docker Compose
* OpenAPI Generator
#### market-service
* Spring WebFlux 
* Thymeleaf
* Spring Data R2DBC
* PostgreSQL
* Redis (кеш товаров)
* JUnit 5, Spring Boot Test, Testcontainers
####  payment-service
* Spring WebFlux
* In-memory модель баланса (демо-версия)
* OpenAPI + WebClient
#### Безопасность
* Аутентификация и авторизация через Keycloak
* Защищённые эндпоинты
* JWT-токены для межсервисного взаимодействия
---
###  Структура проекта 

      root
      ├── market-service/
      │    ├── config/                    # Конфигурационные классы Spring
      │    ├── controller/
      │    │   ├── ApiController.java      # Основной контроллер для страниц
      │    │   └── exception               # Глобальный обработчик ошибок
      │    |
      │    ├── dto/                        # DTO и мапперы
      │    │   └── mapperDto
      │    │    
      │    ├── entity/                     # Сущности JPA
      │    │   
      │    ├── repository/                 # Репозитории Spring Data JPA
      │    │   
      │    ├── service/                    # Сервисы для работы с сущностями
      │    │   
      │    ├── MarketAppApplication.java   # Класс запуска Spring Boot
      │    |
      │    resources/
      │    ├── templates/                  # HTML шаблоны Thymeleaf
      │    ├── static/images/              # Статические ресурсы (изображения)    
      │    │  
      │    ├── db/changelog/               # Скрипты миграций базы данных (liquibase)
      │    ├── application.yaml            # Конфигурация Spring Boot
      │    |
      │    test/                           # Тесты Spring MVC и Spring Data Jpa
      │
      ├── payment-service/ 
      │     ├── PaymentAppApplication.java  # Класс запуска Spring Boot
      │     ├── PaymentApiController.java   # Контроллер оплаты
      │     ├── SecurityConfig.java         # Конфигурация безопасности
      │     ├── resource/
      │     │   └── application.yaml         # Конфигурация Spring Boot
      │     └── test/                        # Тесты контроллера оплаты
      │
      ├── docker-compose.yml     # Запуск всех сервисов
      ├── build.gradle
      └── settings.gradle


### Взаимодействие сервисов

* market-service обращается к payment-service по HTTP (OpenAPI client)
* payment-service инкапсулирует логику проверки баланса и списания средств
* каждый сервис запускается как отдельное Spring Boot приложение

---

## Особенности приложения
### Доступно без авторизации
1. **Витрина товаров**:
    - GET `/` или `/items`
    - Параметры: `search`, `sort`, `pageNumber`, `pageSize`
    - Возвращает страницу `items.html` с пагинацией и сортировкой
    * Данные берутся из Redis, при отсутствии — загружаются из БД и кешируются
    * Доступно только для просмотра 
   
2. **Страница товара**:
    - GET `/items/{id}`
    - Параметр: `id` — идентификатор товара
    - Возвращает страницу `item.html`
   * Данные берутся из Redis, при отсутствии — из БД
   * Доступно для просмотра

> Примечание: Для неавторизованных пользователей функциональность ограничена — кнопки добавления в корзину, управления корзиной и оформления заказа неактивны.

### Требуется авторизация
3. **Корзина товаров**:
    - GET `/cart/items` — список товаров в корзине
    - POST `/cart/items` — изменение количества товара (`PLUS`, `MINUS`, `DELETE`)
   * Данные товаров — из Redis

4. **Заказы**:
    - GET `/orders` — список всех заказов
    - GET `/orders/{id}` — конкретный заказ
    - POST `/buy` — оформление заказа с проверкой баланса через payment-service

5. **Данные товаров**:
    - Содержат поля: `id`, `title`, `description`, `imgPath`, `price`, `count`

> Примечание: Все операции с корзиной и заказами требуют авторизации через Keycloak. Пользователи могут просматривать и управлять своей корзиной, а также оформлять заказы только после успешного входа в систему.

6. **Тесты**:
    - Unit- и интеграционные тесты сервисов и контроллеров
    - Использование Spring Boot Test, JUnit 5 и Testcontainers

### Основные страницы:
- `items.html` — главная страница каталога товаров (`/`, `/items`)
- `item.html` — страница конкретного товара (`/items/{id}`)
- `cart.html` — страница корзины (`/cart/items`)
- `orders.html` — страница со списком заказов (`/orders`)
- `order.html` — страница конкретного заказа (`/orders/{id}`)

### Кеширование (Redis)
* Используется Spring Cache + Redis
* TTL данных о товарах: настраиваемый (по умолчанию 1–2 минуты)
* Кешируются:
   * карточки товаров
   * списки товаров
   * данные корзины

---

## Сборка и запуск

### Локальная сборка Gradle
Собрать все подпроекты:
```bash
   ./gradlew clean build
```
Собрать отдельный сервис:
```bash
   ./gradlew :market-service:build
   ./gradlew :payment-service:build
```
Запуск локально:
```bash
   ./gradlew :market-service:bootRun
   ./gradlew :payment-service:bootRun
```
Сгенерировать jar и запустить:
```bash
   java -jar market-service/build/libs/market-service.jar
   java -jar payment-service/build/libs/payment-service.jar
```

### Сборка docker образа
```bash
   docker build -t market-app .
```
или
```bash
   ./gradlew bootBuildImage --imageName=market-app
```

### Запуск через Docker Compose
Проект полностью поднимается одной командой:
```bash
   docker-compose up --build
```
Будут запущены:
* PostgreSQL
* Redis
* Keycloak (авторизация)
* market-service
* payment-service

Доступные адреса
* Market UI: http://localhost:8080
* Payment API: http://localhost:8081

## Тестирование
```bash
./gradlew test
```

