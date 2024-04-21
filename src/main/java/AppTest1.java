import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.*;

public class AppTest1 {
    public static void main(String[] args) throws Exception {
        String jsonStr = """
            {     
                "data": {
                    "list": [
                        {
                            "attr1": "value1",
                            "attr2": "value2",
                            "attr3": [
                                {"depth": "valueX"}
                            ]
                        },
                        {
                            "attr1": "valueX",
                            "attr2": "value2",
                            "attr3": [
                                {"depth": "valueY"}
                            ]
                        },
                        {
                            "attr1": "value1",
                            "attr2": "value2",
                            "attr3": [
                                {"depth": "value1"}
                            ]
                        }
                    ]
                }   
            }                                         
                """;

        String jsonStr2 = """
                {
                    "data": {
                        "list": [
                            {"attr1": "value1"},
                            {"attr1": "valueX"},
                            {"attr1": "value1"}
                        ]
                    }
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode jsonNode = objectMapper.readTree(jsonStr);

        String[] fields = {"data", "list", "attr3", "depth"};

        filterV2(jsonNode, fields, "value1");
    }

    public static boolean isJsonNodeNullOrIsNull(JsonNode jsonNode) {
        return jsonNode == null || jsonNode.isNull();
    }

    public static void filter(Iterator<JsonNode> parentIterator, JsonNode jsonNode, String[] fields, String value) {
        JsonNode currentFieldNode = jsonNode;

        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            currentFieldNode = currentFieldNode.get(field);

            if (isJsonNodeNullOrIsNull(currentFieldNode)) {
                return;
            }

            if (currentFieldNode.isValueNode()) {
                String jsonNodeText = currentFieldNode.asText();

                if (parentIterator != null && value.equals(jsonNodeText)) {
                    parentIterator.remove();
                }

                return;
            }

            if (currentFieldNode.isObject()) {
                continue;
            }

            ArrayNode arrayNode = (ArrayNode) currentFieldNode;
            Iterator<JsonNode> arrayNodeIterator = arrayNode.iterator();

            while (arrayNodeIterator.hasNext()) {
                JsonNode arrayChildNode = arrayNodeIterator.next();

                if (arrayChildNode.isValueNode()) {
                    String jsonNodeText = arrayChildNode.asText();
                    if (value.equals(jsonNodeText)) {
                        arrayNodeIterator.remove();
                    }
                }
                else if (arrayChildNode.isObject()) {
                    filter(arrayNodeIterator, arrayChildNode,
                            Arrays.copyOfRange(fields, i+1, fields.length), value);
                } else if (arrayChildNode.isArray()) {
                    filter(arrayNodeIterator, arrayChildNode,
                            Arrays.copyOfRange(fields, i, fields.length), value);
                }
            }

            break;
        }
    }

    public static void filterV2(JsonNode jsonNode, String[] fields, String value) {
        List<Object> params = new ArrayList<>();
        params.add(null);
        params.add(jsonNode);
        params.add(fields);

        Stack<List<Object>> stack = new Stack<>();
        stack.push(params);

        while (!stack.isEmpty()) {
            List<Object> tmpParams = stack.pop();
            JsonNode parentNode = (JsonNode) tmpParams.get(0);
            JsonNode currentFieldNode = (JsonNode) tmpParams.get(1);
            String[] tmpFields = (String[]) tmpParams.get(2);

            for (int i = 0; i < tmpFields.length; i++) {
                String field = tmpFields[i];
                JsonNode fieldParentNode = currentFieldNode;
                currentFieldNode = currentFieldNode.get(field);

                if (isJsonNodeNullOrIsNull(currentFieldNode)) {
                    break;
                }

                if (currentFieldNode.isValueNode()) {
                    String jsonNodeText = currentFieldNode.asText();

                    if (parentNode != null && value.equals(jsonNodeText)) {
                        removeFromIterator(parentNode, fieldParentNode);
                    }

                    break;
                }

                if (currentFieldNode.isObject()) {
                    continue;
                }

                ArrayNode arrayNode = (ArrayNode) currentFieldNode;
                Iterator<JsonNode> arrayNodeIterator = arrayNode.iterator();

                while (arrayNodeIterator.hasNext()) {
                    JsonNode arrayChildNode = arrayNodeIterator.next();

                    if (arrayChildNode.isValueNode()) {
                        String jsonNodeText = arrayChildNode.asText();
                        if (value.equals(jsonNodeText)) {
                            arrayNodeIterator.remove();
                        }
                    } else if (arrayChildNode.isObject()) {
                        tmpParams = new ArrayList<>();
                        tmpParams.add(arrayNode);
                        tmpParams.add(arrayChildNode);
                        tmpParams.add(Arrays.copyOfRange(tmpFields, i+1, tmpFields.length));

                        stack.push(tmpParams);
                    }
                }

                break;
            }
        }
    }

    public static void removeFromIterator(JsonNode parentNode, JsonNode elem) {
        if (!(parentNode instanceof ArrayNode)) {
            return;
        }

        ArrayNode parentArrayNode = (ArrayNode) parentNode;
        Iterator<JsonNode> iterator = parentArrayNode.iterator();

        while (iterator.hasNext()) {
            JsonNode currentElem = iterator.next();
            if (currentElem == elem) {
                iterator.remove();
            }
        }
    }
}