package com.green.mmg.rider.work.dto;

import com.green.mmg.rider.rider.model.RiderStatus;

/**
 * PUT /api/rider/status 요청 Body — R8 D8-a 토글.
 *
 * <p>허용 값: ACTIVE / EATING. PENDING/SUSPENDED 토글 요청 시 BAD_REQUEST.</p>
 */
public record WorkSessionStatusReq(RiderStatus to) {
}
