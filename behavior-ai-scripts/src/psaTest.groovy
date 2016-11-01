import org.codehaus.groovy.control.CompilerConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class psaTest extends GroovyTestCase {

    void tearDown() {

    }

    private static final String SCRIPT_UNDER_TEST = 'src/psa.groovy'

    private CompilerConfiguration compilerConfiguration
    private Binding binding

    @BeforeMethod
    public void setUp() throws Exception {
        compilerConfiguration = new CompilerConfiguration()
        binding = new Binding()
    }

    @DataProvider
    public Object[][] createTestData() {
        List<Object[]> refdata = new ArrayList<>()
        refdata.add([["A","A","B","A","A","A","B","A","A","A","B","A"], ["A","C","C","B","C","A","A","C","C","B","C","A","A","C","C","B","C","A"], 64L])
        return refdata
    }

    @Test(dataProvider = 'createTestData')
    void 'calculate a custom document score, based on parameters a and b, and documents height'(ArrayList a, ArrayList b, Long expected_score) {
        // given
        binding.setVariable("sequence1", a)
        binding.setVariable("sequence2", b)

        // when
        evaluateScriptUnderTest(this.binding)

        // then
        long score = (long) this.binding.getVariable("_scorePsa")
        assert score == expected_score
    }

    private void evaluateScriptUnderTest(Binding binding) {
        GroovyShell gs = new GroovyShell(binding, compilerConfiguration)
        gs.evaluate(new File(SCRIPT_UNDER_TEST));
    }
}

class MockDocument {
    long height;

    MockDocument(long height) {
        this.height = height
    }
}