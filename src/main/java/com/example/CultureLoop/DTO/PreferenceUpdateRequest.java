package com.example.CultureLoop.DTO;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class PreferenceUpdateRequest {
    private List<String> preference;
}
