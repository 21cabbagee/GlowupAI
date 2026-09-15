from glowupai.openai_ai import _OBSERVATION_SCHEMA


def test_observation_schema_matches_the_strict_result_contract():
    observation = _OBSERVATION_SCHEMA["properties"]["observations"]["items"][
        "properties"
    ]
    assert observation["region"]["enum"] == [
        "forehead",
        "left_cheek",
        "right_cheek",
        "nose",
        "chin",
        "whole_face",
    ]
    assert observation["visibility"]["enum"] == [
        "visible",
        "not_visible",
        "uncertain",
        "not_assessable",
    ]
    assert observation["extent"]["enum"] == [
        "localized",
        "widespread",
        "uncertain",
        "not_assessable",
    ]
