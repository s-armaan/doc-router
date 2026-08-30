# Doc Router

Doc Router automatically organizes new files in your **Downloads** folder. Create rules for the files you receive, leave Doc Router running, and it will move or rename matching files as they arrive.

## Get started

1. Download the Windows installer from the project's [Releases page](../../releases), then install Doc Router.
2. Open Doc Router. The first time it opens, it creates its configuration file and lets you know where to find it:

   `%APPDATA%\doc-router\config.yaml`

3. Open that file in a text editor, replace `rules: []` with your rules, then save it.
4. Restart Doc Router. Keep it running to organize new downloads.

Doc Router has no main window. It quietly watches Downloads and shows a message if it needs your attention.

## Create rules

Rules are written in YAML. Put more specific rules before more general rules: the first rule that matches a file is used.

Here is an example that moves PDF invoices to folders in Downloads organized by year and month, and then files other PDFs in a separate folder:

```yaml
settings: {}

rules:
  - name: invoices
    priority: 100
    when:
      extensions: ["pdf"]
      filenameContains: ["invoice"]
    then:
      moveTo: "Documents/Invoices/{year}/{month}"
      renameAs: "Invoice_{originalName}_{year}-{month}.pdf"

  - name: other-pdfs
    priority: 200
    when:
      extensions: ["pdf"]
    then:
      moveTo: "Documents/PDFs/{year}/{month}"
```

This example routes a file named `July invoice.pdf` to:

`Downloads\Documents\Invoices\2026\08\Invoice_July invoice_2026-08.pdf`

### Rule options

- `extensions`: One or more file types, such as `["pdf", "docx"]`.
- `filenameContains`: One or more words or phrases that can appear in the filename.
- `filenameStartsWith`: Text the filename must begin with.
- `moveTo`: A folder inside Downloads. Missing folders are created automatically.
- `renameAs`: An optional new filename. Include the extension yourself when renaming a file.

You can use these placeholders in `moveTo` and `renameAs`:

- `{year}`: the current four-digit year
- `{month}`: the current two-digit month
- `{originalName}`: the original filename without its extension

Each rule needs a unique `name` and `priority`, at least one matching option under `when`, and at least one action under `then`.

## Important notes

- Only files added to Downloads while Doc Router is running are processed.
- Rules never move a file outside Downloads.
- If a rule does not match a file, Doc Router leaves it where it is.
