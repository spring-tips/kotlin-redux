package com.example.reactive

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.gateway.handler.predicate.RoutePredicates.path
import org.springframework.cloud.gateway.route.gateway
import org.springframework.context.support.beans
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.toFlux

@SpringBootApplication
class ReactiveApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder()
            .sources(ReactiveApplication::class.java)
            .initializers(beans {
                bean {
                    val customerRepository = ref<CustomerRepository>()
                    val customers: Publisher<Customer> = listOf("Margarette", "Matt", "Michelle", "Michael")
                            .toFlux()
                            .map { Customer(name = it) }
                            .flatMap { customerRepository.save(it) }
                    customerRepository
                            .deleteAll()
                            .thenMany(customers)
                            .thenMany(customerRepository.findAll())
                            .subscribe { println(it) }
                }
                bean {
                    gateway {
                        route {
                            id("blog")
                            predicate(path("/blog") or path("/atom"))
                            uri("http://spring.io:80/blog.atom")
                        }
                    }
                }
                bean {
                    router {
                        val customerRepository = ref<CustomerRepository>()
                        GET("/customers") { ServerResponse.ok().body(customerRepository.findAll()) }
                        GET("/customers/{id}") { ServerResponse.ok().body(customerRepository.findById(it.pathVariable("id"))) }
                    }
                }
            })
            .run(*args)
}


interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
data class Customer(@Id var id: String? = null, var name: String? = null)