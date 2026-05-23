package com.green.mmg.main.chatbot.dto;

import com.green.mmg.main.chatbot.entity.EntryPoint;
import com.green.mmg.main.chatbot.entity.ToneMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatSessionStartReq {
    private EntryPoint entryPoint;
    private ToneMode toneMode;
}
