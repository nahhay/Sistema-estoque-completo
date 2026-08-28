package estoque.api;

import java.util.HashMap;
import java.util.Map;

public final class ApiResponse {

    private ApiResponse() {
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("dados", data);
        return map;
    }

    public static Map<String, Object> mensagem(String mensagem) {
        Map<String, Object> map = new HashMap<>();
        map.put("mensagem", mensagem);
        return map;
    }

    public static Map<String, Object> erro(String erro) {
        Map<String, Object> map = new HashMap<>();
        map.put("erro", erro);
        return map;
    }
}
