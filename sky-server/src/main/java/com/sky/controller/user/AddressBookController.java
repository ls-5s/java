package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user/addressBook")
public class AddressBookController {
    @Autowired
    private AddressBookService addressBookService;

    @PostMapping
    public Result save(@RequestBody AddressBook addressBook){
        log.info("新增地址");
        addressBookService.save(addressBook);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<AddressBook>> list(){
        log.info("查询当前登录用户的所有地址信息");
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());

        List<AddressBook> list = addressBookService.list(addressBook);
        return Result.success(list);
    }

    @GetMapping("/default")
    public Result<AddressBook> getDefault(){
        log.info("查询默认地址");
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setIsDefault(1);

        List<AddressBook> list = addressBookService.list(addressBook);
        if (list != null && list.size() == 1) {
            return Result.success(list.get(0));
        }

        return Result.error("没有查询到默认地址");
    }

    @GetMapping("/{id}")
    public Result<AddressBook> getById(@PathVariable Long id){
        log.info("根据id查询地址：{}",id);
        AddressBook addressBook = addressBookService.getById(id);
        return Result.success(addressBook);
    }

    @PutMapping
    public Result update(@RequestBody AddressBook addressBook){
        log.info("修改地址：{}",addressBook);
        addressBookService.update(addressBook);
        return Result.success();
    }

    @PutMapping("/default")
    public Result setDefault(@RequestBody AddressBook addressBook){
        log.info("设置默认地址：{}",addressBook);
        addressBookService.setDefault(addressBook);
        return Result.success();
    }

    @DeleteMapping
    public Result delete(@RequestParam Long id){
        log.info("根据id删除地址：{}",id);
        addressBookService.delete(id);
        return Result.success();
    }
}
