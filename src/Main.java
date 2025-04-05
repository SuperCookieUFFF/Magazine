import java.util.*;
public class Main {
    public static void main(String[] args) {
        // Принцип инверсии зависимостей (DIP): Создаем магазин с внедренным обработчиком ввода.
        ConsoleShop shop = new ConsoleShop();
        shop.run();
    }
}

//region Исключения (ошибки)
class ProductNotFoundException extends Exception {
    public ProductNotFoundException(String message) {
        super(message);
    }
}

class InvalidQuantityException extends Exception {
    public InvalidQuantityException(String message) {
        super(message);
    }
}

class CartEmptyException extends Exception {
    public CartEmptyException(String message) {
        super(message);
    }
}
//endregion

// Принцип открытости/закрытости (OCP): Можно добавлять новые стратегии скидок, реализуя этот интерфейс.
interface DiscountStrategy {
    double applyDiscount(double price);
}

class Product {
    private final String id;
    private final String name;
    private final double price;
    private DiscountStrategy discountStrategy = null; // Скидка по умолчанию отсутствует.

    public Product(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    // Принцип инверсии зависимостей (DIP): Внедрение зависимости через сеттер.
    public void setDiscountStrategy(DiscountStrategy discountStrategy) {
        this.discountStrategy = Objects.requireNonNull(discountStrategy, "Стратегия скидок не может быть нулевой.");
    }

    public double getDiscountedPrice() {
        return discountStrategy != null ? discountStrategy.applyDiscount(price) : price;
    }

    @Override
    public String toString() {
        return String.format("%s - $%.2f", name, price);
    }
}

class Inventory {
    private final Map<String, Product> products = new HashMap<>();

    public void addProduct(Product product) {
        Objects.requireNonNull(product, "Продукт не может быть нулевым.");
        if (products.containsKey(product.getId())) {
            throw new IllegalArgumentException("Продукт с таким идентификатором уже существует.");
        }
        products.put(product.getId(), product);
    }

    public Product getProduct(String productId) throws ProductNotFoundException {
        return Optional.ofNullable(products.get(productId))
                .orElseThrow(() -> new ProductNotFoundException("Товар с ID не найден: " + productId));
    }

    public List<Product> listProducts() {
        return new ArrayList<>(products.values());
    }
}

class ShoppingCart {
    private final Map<String, Integer> items = new HashMap<>();

    public void addItem(String productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным.");
        }
        items.put(productId, items.getOrDefault(productId, 0) + quantity);
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public double calculateTotal(Inventory inventory) throws ProductNotFoundException {
        double total = 0;
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            Product product = inventory.getProduct(productId);
            total += product.getDiscountedPrice() * quantity;
        }
        return total;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }
}

// Принцип единственной ответственности (SRP): Отвечает только за получение ввода пользователя.
class InputHandler {
    private final Scanner scanner = new Scanner(System.in);

    public String getCommand() {
        System.out.print("Введите команду: ");
        return scanner.nextLine().trim();
    }
}

// Принцип единственной ответственности (SRP): Отвечает только за форматирование вывода.
class OutputFormatter {
    public void displayProducts(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("Товары не найдены.");
            return;
        }

        products.forEach(System.out::println); // Use lambda expression for brevity.
    }

    public void displayCart(ShoppingCart cart, Inventory inventory) throws ProductNotFoundException {
        if (cart.isEmpty()) {
            System.out.println("Ваша корзина пуста.");
            return;
        }

        System.out.println("Содержимое корзины:");
        double total = 0;
        for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
            String productId = entry.getKey();
            int quantity = entry.getValue();
            Product product = inventory.getProduct(productId);
            System.out.println(product.getName() + " (x" + quantity + ")");
            total += product.getDiscountedPrice() * quantity;
        }

        System.out.println(String.format("Итого: $%.2f", total));
    }
}

// Принцип инверсии зависимостей (DIP): Зависим от абстракций InputHandler и OutputFormatter.
 class ConsoleShop {
    private static final String EXIT_COMMAND = "выход";
    private final Inventory inventory = new Inventory();
    private final ShoppingCart cart = new ShoppingCart(); // Добавили корзину
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter = new OutputFormatter();

    // Принцип инверсии зависимостей (DIP): Внедрение зависимости через конструктор.
    public ConsoleShop(InputHandler inputHandler) {
        this.inputHandler = inputHandler;
        initializeProducts();
    }

    public ConsoleShop() {
        this(new InputHandler()); // Inject default dependency.
    }

    private void initializeProducts() {
        Product laptop = new Product("1", "Laptop", 1200);
        // Принцип открытости/закрытости (OCP): Легко изменить стратегию скидок, не трогая Product.
        laptop.setDiscountStrategy(price -> price * 0.9); // 10% скидка
        inventory.addProduct(laptop);

        Product mouse = new Product("2", "Mouse", 25);
        inventory.addProduct(mouse);
    }

    public void run() {
        System.out.println("Добро пожаловать в Консольный Магазин!");
        System.out.println("Доступные команды: список, добавить <id>, корзина, оформить, выход");

        while (true) {
            String command = inputHandler.getCommand();
            String[] parts = command.split(" ");
            String action = parts[0].toLowerCase();

            try {
                switch (action) {
                    case EXIT_COMMAND:
                        System.out.println("Выход из программы...");
                        return;
                    case "список":
                        displayProducts();
                        break;
                    case "добавить":
                        addItemToCart(parts);
                        break;
                    case "корзина":
                        viewCart();
                        break;
                    case "оформить":
                        checkout();
                        break;
                    default:
                        System.out.println("Неверная команда.");
                }
            } catch (ProductNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }

    }

    private void checkout() {
        if (cart.isEmpty()) {
            System.out.println("Ваша корзина пуста. Нечего оформлять.");
            return;
        }
        System.out.println("Заказ оформлен! Спасибо за покупку!");
        cart.clear();
    }

    private void viewCart() throws ProductNotFoundException {
        outputFormatter.displayCart(cart, inventory);
    }

    private void addItemToCart(String[] parts) throws ProductNotFoundException {
        if (parts.length != 2) {
            System.out.println("Использование: добавить <id>");
            return;
        }
        String productId = parts[1];
        inventory.getProduct(productId); // Проверяем, что товар существует.
        cart.addItem(productId, 1); // Добавляем один товар в корзину
        System.out.println("Товар добавлен в корзину.");
    }

    public void displayProducts() {
        List<Product> products = inventory.listProducts();
        outputFormatter.displayProducts(products);
    }
}
