package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ItemMapper itemMapper;

    @Override
    public String getItem(String id) {
        Item item = new Item();
        item.setId(id);
        Item ret = itemMapper.search(item);
        if (ret == null) {
            return "";
        }
        return ret.getName();
    }
}
