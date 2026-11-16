package com.secureprint.service;

import com.secureprint.dto.OverlayRequest;
import java.util.List;

public interface EditStrategy {
    byte[] apply(byte[] original, List<OverlayRequest> overlays);
}
