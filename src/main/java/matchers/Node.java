package matchers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Node {
    private Map<Character,Node> next = new HashMap<>();
    private Node fail = null;
    private List<String> outputs = new ArrayList<>();
}
