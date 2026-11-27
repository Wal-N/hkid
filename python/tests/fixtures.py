import json
from pathlib import Path


def load_fixtures(name):
    path = Path(__file__).resolve().parents[2] / "conformance" / f"{name}.json"
    fixtures = json.loads(path.read_text(encoding="utf-8"))
    version = fixtures["schemaVersion"]
    if type(version) is not int or version != 1:
        raise ValueError(f"Unsupported {name} fixture schema version {version!r}")
    for group, cases in fixtures.items():
        if group == "schemaVersion":
            continue
        if not isinstance(cases, list) or not cases:
            raise ValueError(f"{name}.{group} must be a nonempty array")
        seen_ids = set()
        for index, case in enumerate(cases):
            if not isinstance(case, dict) or not isinstance(case.get("id"), str):
                raise ValueError(f"{name}.{group}[{index}] must be an object with a string ID")
            case_id = case["id"]
            if case_id in seen_ids:
                raise ValueError(f"Duplicate case ID {case_id!r} in {name}.{group}")
            seen_ids.add(case_id)
            for field in ("expectValid", "expectSameValue"):
                if field in case and not isinstance(case[field], bool):
                    raise ValueError(f"{name}.{group}[{case_id!r}].{field} must be a boolean")
    return fixtures
