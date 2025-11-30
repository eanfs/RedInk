package api

import (
	"context"
	"encoding/json"
	"fmt"

	"encoding/base64"
	"strings"

	"redink-api/service"

	"github.com/cloudwego/hertz/pkg/app"
	"github.com/cloudwego/hertz/pkg/protocol/consts"
)

type OutlineRequest struct {
	Topic  string   `json:"topic"`
	Images []string `json:"images"` // Base64 or URL
}

func GenerateOutlineHandler(c context.Context, ctx *app.RequestContext) {
	var req OutlineRequest
	if err := ctx.BindAndValidate(&req); err != nil {
		ctx.JSON(consts.StatusBadRequest, map[string]interface{}{
			"success": false,
			"error":   err.Error(),
		})
		return
	}

	if req.Topic == "" {
		ctx.JSON(consts.StatusBadRequest, map[string]interface{}{
			"success": false,
			"error":   "Topic is required",
		})
		return
	}

	result := service.Outline.GenerateOutline(c, req.Topic, req.Images)
	if !result.Success {
		ctx.JSON(consts.StatusInternalServerError, result)
		return
	}
	ctx.JSON(consts.StatusOK, result)
}

// Stubs for other handlers
type Page struct {
	Index   int    `json:"index"`
	Type    string `json:"type"`
	Content string `json:"content"`
}

type GenerateImagesRequest struct {
	Pages       []Page   `json:"pages"`
	TaskID      string   `json:"task_id"`
	FullOutline string   `json:"full_outline"`
	UserTopic   string   `json:"user_topic"`
	UserImages  []string `json:"user_images"` // Base64
}

func GenerateImagesHandler(c context.Context, ctx *app.RequestContext) {
	var req GenerateImagesRequest
	if err := ctx.BindAndValidate(&req); err != nil {
		ctx.JSON(consts.StatusBadRequest, map[string]interface{}{
			"success": false,
			"error":   err.Error(),
		})
		return
	}

	if len(req.Pages) == 0 {
		ctx.JSON(consts.StatusBadRequest, map[string]interface{}{
			"success": false,
			"error":   "Pages are required",
		})
		return
	}

	ctx.SetContentType("text/event-stream")
	ctx.Response.Header.Set("Cache-Control", "no-cache")
	ctx.Response.Header.Set("X-Accel-Buffering", "no")

	// Convert pages
	var pages []service.ImagePage
	for _, p := range req.Pages {
		pages = append(pages, service.ImagePage{
			Index:   p.Index,
			Type:    p.Type,
			Content: p.Content,
		})
	}

	// Decode user images
	var userImages [][]byte
	for _, b64 := range req.UserImages {
		if idx := strings.Index(b64, ","); idx != -1 {
			b64 = b64[idx+1:]
		}
		if data, err := base64.StdEncoding.DecodeString(b64); err == nil {
			userImages = append(userImages, data)
		}
	}

	ch := service.Image.GenerateImages(c, pages, req.TaskID, req.FullOutline, userImages, req.UserTopic)

	w := ctx.Response.BodyWriter()
	for event := range ch {
		dataBytes, _ := json.Marshal(event.Data)
		fmt.Fprintf(w, "event: %s\n", event.Event)
		fmt.Fprintf(w, "data: %s\n\n", string(dataBytes))
		if f, ok := w.(interface{ Flush() error }); ok {
			f.Flush()
		}
	}
}

func GetImageHandler(c context.Context, ctx *app.RequestContext) {
	ctx.JSON(consts.StatusOK, map[string]string{"message": "Not implemented yet"})
}

func RetrySingleImageHandler(c context.Context, ctx *app.RequestContext) {
	ctx.JSON(consts.StatusOK, map[string]string{"message": "Not implemented yet"})
}

func RetryFailedImagesHandler(c context.Context, ctx *app.RequestContext) {
	ctx.JSON(consts.StatusOK, map[string]string{"message": "Not implemented yet"})
}

func RegenerateImageHandler(c context.Context, ctx *app.RequestContext) {
	ctx.JSON(consts.StatusOK, map[string]string{"message": "Not implemented yet"})
}

func GetTaskStateHandler(c context.Context, ctx *app.RequestContext) {
	ctx.JSON(consts.StatusOK, map[string]string{"message": "Not implemented yet"})
}

// History stubs
func CreateHistoryHandler(c context.Context, ctx *app.RequestContext)      {}
func ListHistoryHandler(c context.Context, ctx *app.RequestContext)        {}
func GetHistoryHandler(c context.Context, ctx *app.RequestContext)         {}
func UpdateHistoryHandler(c context.Context, ctx *app.RequestContext)      {}
func DeleteHistoryHandler(c context.Context, ctx *app.RequestContext)      {}
func SearchHistoryHandler(c context.Context, ctx *app.RequestContext)      {}
func GetHistoryStatsHandler(c context.Context, ctx *app.RequestContext)    {}
func ScanTaskHandler(c context.Context, ctx *app.RequestContext)           {}
func ScanAllTasksHandler(c context.Context, ctx *app.RequestContext)       {}
func DownloadHistoryZipHandler(c context.Context, ctx *app.RequestContext) {}

// Config stubs
func GetConfigHandler(c context.Context, ctx *app.RequestContext)      {}
func UpdateConfigHandler(c context.Context, ctx *app.RequestContext)   {}
func TestConnectionHandler(c context.Context, ctx *app.RequestContext) {}
