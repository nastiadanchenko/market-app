
## Витрина интернет-магазина (Spring Boot)

Проект представляет собой веб-приложение «Витрина интернет-магазина» с функционалом просмотра товаров, управления корзиной и оформлением заказов.

###  Стек технологий
* Java 21
* Spring Boot 3.5.7
* Spring Web MVC
* Thymeleaf (HTML-шаблоны)
* Spring Data JPA + Hibernate
* PostgreSQL 
* Docker
* JUnit 5, Spring Boot Test, Testcontainers
* Gradle
---
###  Структура проекта 
    yandex/workshop/market/
    ├── controller/
    │   ├── ApiController.java      # Основной контроллер для страниц
    │   └── exception               # Глобальный обработчик ошибок
    |
    ├── dto/                        # DTO и мапперы
    │   └── mapperDto
    │    
    ├── entity/                     # Сущности JPA
    │   
    ├── repository/                 # Репозитории Spring Data JPA
    │   
    ├── service/                    # Сервисы для работы с сущностями
    │   
    ├── MarketAppApplication.java   # Класс запуска Spring Boot
    |
    resources/
    ├── templates/                  # HTML шаблоны Thymeleaf
    ├── static/images/              # Статические ресурсы (изображения)    
    │  
    ├── db/changelog/               # Скрипты миграций базы данных (liquibase)
    ├── application.yaml            # Конфигурация Spring Boot
    |
    test/                           # Тесты Spring MVC и Spring Data Jpa

---

## Особенности приложения

1. **Витрина товаров**:
    - GET `/` или `/items`
    - Параметры: `search`, `sort`, `pageNumber`, `pageSize`
    - Возвращает страницу `items.html` с пагинацией и сортировкой

2. **Страница товара**:
    - GET `/items/{id}`
    - Параметр: `id` — идентификатор товара
    - Возвращает страницу `item.html`

3. **Корзина товаров**:
    - GET `/cart/items` — список товаров в корзине
    - POST `/cart/items` — изменение количества товара (`PLUS`, `MINUS`, `DELETE`)

4. **Заказы**:
    - GET `/orders` — список всех заказов
    - GET `/orders/{id}` — конкретный заказ
    - POST `/buy` — оформление нового заказа

5. **Данные товаров**:
    - Содержат поля: `id`, `title`, `description`, `imgPath`, `price`, `count`

6. **Тесты**:
    - Unit- и интеграционные тесты сервисов и контроллеров
    - Использование Spring Boot Test, JUnit 5 и Testcontainers

### Основные страницы:

- `items.html` — главная страница каталога товаров (`/`, `/items`)
- `item.html` — страница конкретного товара (`/items/{id}`)
- `cart.html` — страница корзины (`/cart/items`)
- `orders.html` — страница со списком заказов (`/orders`)
- `order.html` — страница конкретного заказа (`/orders/{id}`)

---

## Сборка и запуск

### Локальная сборка Gradle
```bash
./gradlew clean build
```
### Сборка docker образа
```bash
   docker build -t market-app .
```
или
```bash
   ./gradlew bootBuildImage --imageName=market-app
```

### Запустить приложение:
```bash
./gradlew bootRun
````
или 
```bash
java -jar build/libs/market-app-0.0.1-SNAPSHOT.jar
````
Приложение будет доступно по адресу: http://localhost:8080

### Запуск docker контейнера

```bash
   docker run -p 8080:8080 --name market-app market-app
```

## Тестирование
```bash
./gradlew test
```

