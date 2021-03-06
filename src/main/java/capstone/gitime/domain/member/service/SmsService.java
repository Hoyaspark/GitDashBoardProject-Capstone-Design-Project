package capstone.gitime.domain.member.service;

import capstone.gitime.api.controller.dto.SmsRequestDto;
import capstone.gitime.domain.member.service.dto.NaverMessageDto;
import capstone.gitime.domain.member.service.dto.NaverSmsRequestDto;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${sms.access-key}")
    private String ACCESS_KEY;

    @Value("${sms.service-id}")
    private String SERVICE_ID;

    @Value("${sms.secret-key}")
    private String SECRET_KEY;


    public void smsSend(SmsRequestDto requestDto) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        System.out.println("SERVICE_ID = " + SERVICE_ID);
        System.out.println("ACCESS_KEY = " + ACCESS_KEY);
        System.out.println("SECRET_KEY = " + SECRET_KEY);
        ResponseEntity<String> responseEntity = restTemplate.exchange("https://sens.apigw.ntruss.com/sms/v2/services/" + SERVICE_ID + "/messages",
                HttpMethod.POST, createHttpEntity(requestDto), String.class);
    }
    private HttpEntity<NaverSmsRequestDto> createHttpEntity(SmsRequestDto requestDto) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.valueOf(MediaType.APPLICATION_JSON_VALUE));

        String now = String.valueOf(System.currentTimeMillis());

        httpHeaders.add("x-ncp-apigw-timestamp", now);
        httpHeaders.add("x-ncp-iam-access-key", ACCESS_KEY);
        httpHeaders.add("x-ncp-apigw-signature-v2", makeSignature(now));

        HttpEntity<NaverSmsRequestDto> httpEntity = new HttpEntity(new NaverSmsRequestDto("SMS", "COMM", "82", "01022539477", requestDto.getCode(), List.of(new NaverMessageDto(requestDto.getNum(), null))), httpHeaders);
        return httpEntity;
    }

    public String makeSignature(String now) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String space = " ";					// one space
        String newLine = "\n";					// new line
        String method = "POST";					// method
        String url = "/sms/v2/services/" + SERVICE_ID + "/messages";	// url (include query string)
        String timestamp = now;			// current timestamp (epoch)
        String accessKey = ACCESS_KEY;			// access key id (from portal or Sub Account)
        String secretKey = SECRET_KEY;

        String message = new StringBuilder()
                .append(method)
                .append(space)
                .append(url)
                .append(newLine)
                .append(timestamp)
                .append(newLine)
                .append(accessKey)
                .toString();

        SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);

        byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
        String encodeBase64String = Base64.encodeBase64String(rawHmac);

        return encodeBase64String;
    }
}
