package com.blink.base.dto.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class QueryShowMenuReq implements Serializable {

    @NotEmpty
    private List<Integer> roleIds;


}
