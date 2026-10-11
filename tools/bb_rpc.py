"""Small local MCP transport for the user's configured Blockbench server."""
import argparse
import base64
import json
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent / "blockbench"
ROOT.mkdir(parents=True, exist_ok=True)
SESSION = ROOT / "session.json"
URL = "http://localhost:3000/bb-mcp"


def request(method, params=None, session=None, notification=False):
    payload = {"jsonrpc": "2.0", "method": method}
    if not notification:
        payload["id"] = 1
    if params is not None:
        payload["params"] = params
    headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}
    if session:
        headers["mcp-session-id"] = session
    req = urllib.request.Request(URL, json.dumps(payload).encode(), headers=headers)
    with urllib.request.urlopen(req, timeout=45) as response:
        body = response.read().decode("utf-8")
        result = json.loads(body) if body.strip() else {}
        if "error" in result:
            raise RuntimeError(result["error"])
        return result.get("result", result), response.headers.get("mcp-session-id", session)


def initialize():
    result, session = request("initialize", {
        "protocolVersion": "2024-11-05", "capabilities": {},
        "clientInfo": {"name": "Royale-Witch-art-work", "version": "1.0"},
    })
    SESSION.write_text(json.dumps({"id": session, "server": result}), encoding="utf-8")
    request("notifications/initialized", session=session, notification=True)
    return result, session


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("method")
    parser.add_argument("--params")
    parser.add_argument("--params-b64")
    parser.add_argument("--out")
    parser.add_argument("--brief", action="store_true")
    args = parser.parse_args()
    if args.method == "initialize":
        result, _ = initialize()
    else:
        session = json.loads(SESSION.read_text())["id"] if SESSION.exists() else initialize()[1]
        params = json.loads(pathlib.Path(args.params).read_text(encoding="utf-8-sig")) if args.params else None
        if args.params_b64:
            params = json.loads(base64.b64decode(args.params_b64).decode("utf-8"))
        result, _ = request(args.method, params, session)
    if args.out:
        pathlib.Path(args.out).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    if args.brief and "tools" in result:
        result = [{"name": t["name"], "description": t.get("description", "")} for t in result["tools"]]
    elif args.brief:
        result = {"saved": args.out, "isError": result.get("isError", False),
                  "content": [{k: v for k, v in c.items() if k != "data"} for c in result.get("content", [])]}
    print(json.dumps(result, ensure_ascii=True))


if __name__ == "__main__":
    main()
