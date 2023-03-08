package com.inkneko.heimusic.controller;

import io.lettuce.core.output.KeyValueStreamingChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@RestController
@RequestMapping(value="/users")
public class UserController {

    @PostMapping
    public String save(){
        return "";
    }

    @PutMapping(value = "/{id}")
    public String update(@PathVariable Integer id){
        return "you are updating id: " + id;
    }

    @GetMapping
    public String get(){
        return "all user";
    }

    @GetMapping("/{id}")
    public String get(@PathVariable Integer id){
        return "user: " + id;
    }
}
