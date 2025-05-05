package com.example.CultureLoop.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class User {

    private Long id;

    private String email;
    private String name;
    private String preference; // ex. 취향, 관심사 등
}

