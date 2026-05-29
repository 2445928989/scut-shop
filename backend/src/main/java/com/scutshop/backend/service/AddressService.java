package com.scutshop.backend.service;

import com.scutshop.backend.mapper.AddressMapper;
import com.scutshop.backend.model.Address;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {
    private final AddressMapper mapper;

    public AddressService(AddressMapper mapper) {
        this.mapper = mapper;
    }

    public List<Address> listByUser(Long userId) {
        return mapper.selectByUserId(userId);
    }

    public Address getById(Long id) {
        return mapper.selectById(id);
    }

    public int create(Address a) {
        if (a.getIsDefault() != null && a.getIsDefault() == 1) {
            mapper.clearDefault(a.getUserId());
        }
        return mapper.insert(a);
    }

    public int update(Address a) {
        if (a.getIsDefault() != null && a.getIsDefault() == 1) {
            mapper.clearDefault(a.getUserId());
        }
        return mapper.update(a);
    }

    public int delete(Long id, Long userId) {
        return mapper.delete(id, userId);
    }
}
