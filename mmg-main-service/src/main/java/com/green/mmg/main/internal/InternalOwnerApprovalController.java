package com.green.mmg.main.internal;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.exception.ResourceNotFoundException;
import com.green.mmg.main.internal.dto.InternalOwnerApprovalDetailRes;
import com.green.mmg.main.store.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/store")
@RequiredArgsConstructor
public class InternalOwnerApprovalController {
    private final StoreMapper storeMapper;

    @Transactional(readOnly = true)
    @GetMapping("/owner/{ownerNo}/approval")
    public ResultResponse<InternalOwnerApprovalDetailRes> getOwnerApprovalDetail(@PathVariable long ownerNo){
        InternalOwnerApprovalDetailRes result = storeMapper.findOwnerApprovalDetail(ownerNo);

        if(result == null) {
            throw new ResourceNotFoundException("store not found:" + ownerNo);
        }
        return new ResultResponse<>("사장 승인 상세조회 완료", result);
    }

}
