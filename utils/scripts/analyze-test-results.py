import xml.etree.ElementTree as ElementTree
import os


def failed_tests(filename):
    tree = ElementTree.parse(filename)
    root = tree.getroot()

    failed_testcases = []
    for testcase in root.findall(".//testcase"):
        # failed testcases have a child tag <failure>
        if testcase.find('failure') is not None:
            body = testcase.find('failure').text
            failed_testcases.append((testcase.attrib['name'], body))

    return failed_testcases


folder = "target/test-reports"
for filename in os.listdir(folder):
    failed_tests_in_file = failed_tests(os.path.join(folder, filename))
    if failed_tests_in_file:
        print(f"Failed tests in {filename}:")
        for name, body in failed_tests_in_file:
            body_escaped = "\n  ".join([line for line in body.split("\n") if not line.strip().startswith("at ")]).strip()
            print(f"- Test Name: {name}\n  {body_escaped}")
        print()