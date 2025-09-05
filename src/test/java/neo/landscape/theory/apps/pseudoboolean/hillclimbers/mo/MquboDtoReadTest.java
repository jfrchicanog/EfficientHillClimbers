package neo.landscape.theory.apps.pseudoboolean.hillclimbers.mo;

import com.google.gson.Gson;
import neo.landscape.theory.apps.pseudoboolean.problems.mo.MquboDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class MquboDtoReadTest {

    @Test
    public void testRead() {
        Gson gson = new Gson();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("mQUBOi_1005.json");
             Reader reader = new InputStreamReader(is)) {
            MquboDto data = gson.fromJson(reader, MquboDto.class);
            System.out.println(data.problem.n);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
