package ru.itmo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "products")
public record Product(@Id String id, String name, long price, String currency) {
}
