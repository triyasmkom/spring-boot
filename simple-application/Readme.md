# Simple Application

## Setup Spring Boot
Pertama kita dapat menggunakan https://start.spring.io/ untuk menghasilkan dasar proyek kita.

Kemudian tambahkan dependensi ini:

```gradle
dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	implementation 'org.springframework.boot:spring-boot-starter-web'
	runtimeOnly 'com.h2database:h2'
	
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

## Konfigurasi Aplikasi

Selanjutnya, kita akan mengkonfigurasi kelas utama untuk aplikasi kita:

File ini berada di folder ```src/main/java/com.ths.simpleapplication```.

```java
@SpringBootApplication
public class SimpleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleApplication.class, args);
	}

}
```

Kemudian kita akan mendefinisikan beberapa konfigurasi pada file ```application.properties```, untuk saat ini kita akan mengkonfigurasi port yang akan digunakan.

```properties
server.port=8081
```

Untuk melihat lebih banyak lagi konfigurasi bisa kunjungi link https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-application-properties.html .


## Tampilan MVC Sederhana

Sekarang mari kita tambahkan front end sederhana menggunakan Thymeleaf. Mari kita tambahkan dependensi berikut ini:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
```

Itu mengaktifkan Thymeleaf secara default. Tidak diperlukan konfigurasi tambahan.

Sekarang mari kita konfigurasi di application.properties:

```properties
spring.thymeleaf.cache=false
spring.thymeleaf.enabled=true 
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html

spring.application.name=Bootstrap Spring Boot
```

Selanjutnya kita buat controller:

```java
@Controller
public class SimpleController {

    @Value("${spring.application.name}")
    String appName;

    @GetMapping("/")
    public String homePage(Model model){
        model.addAttribute("appName", appName);
        return "home";
    }
}
```


dan kita buat file home.html

```html
<html>
<head><title>Home Page</title></head>
<body>
<h1>Hello !</h1>
<p>Welcome to <span th:text="${appName}">Our App</span></p>
</body>
</html>
```


## Keamanan

Selanjutnya mari kita tambahkan keamanan pada aplikasi kita terlebih dahulu dengan menambhkan dependensi berikut ini:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
```

Setelah depedensi terpasang, semua endpoint telah diamankan secara default, menggunakan httpBasic atau formLogin berdasarkan strategi Spring Security.

```java

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and().csrf()
                .disable();

        return http.build();
    }
}
```

Ini adalah contoh konfigurasi keamanan. Untuk mengetahui lebih lanjut kunjungi link https://www.baeldung.com/security-spring .


## Persistence

Mari kita definisikan model data kita,


```java
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(nullable = false)
    private String author;

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}

```

Repository:

```java
public interface BookRepository extends CrudRepository<Book, Long> {
    List<Book> findByTitle(String title);
}
```

Kemudian kita konfigurasi:

```java
@EnableJpaRepositories("com.ths.simpleapplication.repository")
@EntityScan("com.ths.simpleapplication.entity")
@SpringBootApplication
public class SimpleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleApplication.class, args);
	}

}
```

- ```@EnableJpaRepositories``` untuk memindai paket yang ditentukan untuk repositori
- ```@EntityScan``` untuk mengambil entitas JPA kami

Untuk mempermudah, pada praktik ini kita menggunakan basis data dalam memori H2.

Setelah menambahkan depedensi H2, Spring Boot akan mendeteksinya secara otomatis dan menyiapkan persistance tanpa memerlukan konfigurasi tambahan, selain sumber data:

```properties
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.url=jdbc:h2:mem:bootapp;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
```

Untuk mengetahui lebih detail bisa kunjungi https://www.baeldung.com/persistence-with-spring-series



## Web dan Pengontrol

Selanjutnya mari kita menyiapkan pengontrolnya.

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired private BookRepository bookRepository;

    @GetMapping
    public Iterable findAll(){
        return bookRepository.findAll();
    }

    @GetMapping("/title/{bookTitle}")
    private List findByTitle(@PathVariable String bookTitle){
        return bookRepository.findByTitle(bookTitle);
    }

    @GetMapping("/{id}")
    public Book findOne(@PathVariable Long id){
        return bookRepository.findById(id).orElseThrow(BookNotFoundException::new);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Book create(@RequestBody Book book){
        return bookRepository.save(book);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        bookRepository.findById(id).orElseThrow(BookNotFoundException::new);
        bookRepository.deleteById(id);
    }

    @PutMapping("/{id}")
    public Book updateBook(@RequestBody Book book, @PathVariable Long id){
        if(book.getId() != id){
            throw new BookIdMismatchException();
        }

        bookRepository.findById(id).orElseThrow(BookNotFoundException::new);
        return bookRepository.save(book);
    }
    
}
```


## Penanganan Kesalahan

```java
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({BookNotFoundException.class})
    protected ResponseEntity<Object> handleNotFound(Exception ex, WebRequest request){
        return handleExceptionInternal(
                ex,
                "Book not found",
                new HttpHeaders(),
                HttpStatus.NOT_FOUND,
                request);
    }

    @ExceptionHandler({BookIdMismatchException.class, ConstraintViolationException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Object> handlerBadRequest(Exception ex, WebRequest request){
        return handleExceptionInternal(
                ex,
                ex.getLocalizedMessage(),
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                request);
    }
}

```


```java
public class BookIdMismatchException extends RuntimeException{
    public BookIdMismatchException(){
        super();
    }

    public BookIdMismatchException(String message, Throwable cause){
        super(message, cause);
    }

    public BookIdMismatchException(String message){
        super(message);
    }

    public BookIdMismatchException(Throwable cause){
        super(cause);
    }
}

```

```java
public class BookNotFoundException extends RuntimeException{
    public BookNotFoundException(){
        super();
    }
    public BookNotFoundException(String message, Throwable cause){
        super(message, cause);
    }

    public BookNotFoundException(String message){
        super(message);
    }
    public BookNotFoundException(Throwable cause){
        super(cause);
    }
}

```


## Pengujian
Mari kita tambahkan depedensi berikut ini:

```gradle
testImplementation 'io.rest-assured:rest-assured:5.3.1'
testImplementation 'org.hamcrest:hamcrest-all:1.1'
```

```java
@SpringBootTest
public class BootstrapLiveTest {
    private static final String API_ROOT = "http://localhost:8081/api/books";

    private Book createRandomBook(){
        Book book = new Book();
        book.setTitle(randomAlphabetic(10));
        book.setAuthor(randomAlphabetic(15));
        return book;
    }

    private String createBookAsUri(Book book){
        Response response = RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(book)
                .post(API_ROOT);
        return API_ROOT+"/"+response.jsonPath().get("id");
    }

    @Test
    public void whenGetAllBooks_thenOK(){
        Response response = RestAssured.get(API_ROOT);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
    }

    @Test
    public void whenGetBooksByTitle_thenOK() {
        Book book = createRandomBook();
        createBookAsUri(book);
        Response response = RestAssured.get(
                API_ROOT + "/title/" + book.getTitle());

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertTrue(response.as(List.class).size() > 0);
    }
    @Test
    public void whenGetCreatedBookById_thenOK() {
        Book book = createRandomBook();
        String location = createBookAsUri(book);
        Response response = RestAssured.get(location);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals(book.getTitle(), response.jsonPath()
                .get("title"));
    }

    @Test
    public void whenGetNotExistBookById_thenNotFound() {
        Response response = RestAssured.get(API_ROOT + "/" + randomNumeric(4));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode());
    }

//    Create Book
    @Test
    public void whenCreateNewBook_thenCreated() {
        Book book = createRandomBook();
        Response response = RestAssured
                .given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(book)
                .post(API_ROOT);

        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode());
    }

    @Test
    public void whenInvalidBook_thenError() {
        Book book = createRandomBook();
        book.setAuthor(null);
        Response response = RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(book)
                .post(API_ROOT);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
    }


//    Update Book
    @Test
    public void whenUpdateCreatedBook_thenUpdated() {
        Book book = createRandomBook();
        String location = createBookAsUri(book);
        book.setId(Long.parseLong(location.split("api/books/")[1]));
        book.setAuthor("newAuthor");
        Response response = RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(book)
                .put(location);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());

        response = RestAssured.get(location);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("newAuthor", response.jsonPath()
                .get("author"));
    }

//    Delete Book
    @Test
    public void whenDeleteCreatedBook_thenOk() {
        Book book = createRandomBook();
        String location = createBookAsUri(book);
        Response response = RestAssured.delete(location);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());

        response = RestAssured.get(location);
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode());
    }

}

```


### References:
- https://www.baeldung.com/spring-boot-start
- https://www.baeldung.com/security-spring
