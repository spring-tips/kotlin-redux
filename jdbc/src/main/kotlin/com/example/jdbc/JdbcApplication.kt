package com.example.jdbc

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.*
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.support.beans
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class JdbcApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder()
            .initializers(beans {
                bean { SpringTransactionManager(ref()) }
                bean {
                    ApplicationRunner {
                        val customerService = ref<CustomerService>()
                        arrayOf("Tammie", "Mario", "Andrew", "Cornelia")
                                .map { Customer(name = it) }
                                .forEach { customerService.insert(it) }
                        customerService.all().forEach { println(it) }
                    }
                }
            })
            .sources(JdbcApplication::class.java)
            .run(*args)
}

@RestController
class CustomerRestController(private val customerService: CustomerService) {

    @GetMapping("/customers")
    fun customers() = this.customerService.all()
}

@Service
@Transactional
class ExposedCustomerService(private val transactionTemplate: TransactionTemplate) : CustomerService, InitializingBean {

    override fun afterPropertiesSet() {
        this.transactionTemplate.execute {
            SchemaUtils.create(Customers)
        }
    }

    override fun all(): Collection<Customer> = Customers.selectAll().map { Customer(it[Customers.name], it[Customers.id]) }

    override fun byId(id: Long): Customer? = Customers
            .select { Customers.id.eq(id) }
            .map { Customer(it[Customers.name], it[Customers.id]) }
            .firstOrNull()

    override fun insert(customer: Customer) {
        Customers.insert { it[Customers.name] = customer.name }
    }
}

object Customers : Table() {
    val id = long("ID").autoIncrement().primaryKey()
    val name = varchar("NAME", 255)
}

/*
@Service
@Transactional
class JdbcTemplateCustomerService(private val jdbcTemplate: JdbcTemplate) : CustomerService {

    override fun all(): Collection<Customer> = this.jdbcTemplate.query("SELECT * FROM CUSTOMERS") { rs, _ ->
        Customer(rs.getString("NAME"), rs.getLong("ID"))
    }

    override fun byId(id: Long): Customer? = this.jdbcTemplate.queryForObject("SELECT * FROM CUSTOMERS where ID=?", id) { rs, _ ->
        Customer(rs.getString("NAME"), rs.getLong("ID"))
    }

    override fun insert(customer: Customer) {
        this.jdbcTemplate.execute("INSERT INTO CUSTOMERS(NAME) VALUES(?)") {
            it.setString(1, customer.name)
            it.execute()
        }
    }
}
*/


interface CustomerService {
    fun all(): Collection<Customer>
    fun byId(id: Long): Customer?
    fun insert(customer: Customer)
}

data class Customer(val name: String, var id: Long? = null)